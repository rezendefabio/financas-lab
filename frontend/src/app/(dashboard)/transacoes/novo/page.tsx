'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { contasService } from '@/features/contas/services/contas.service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { listarPayees } from '@/features/payee/services/payee-service'
import { listarTags } from '@/features/tag/services/tag-service'
import { Card, CardContent } from '@/shared/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const STATUS_OPTIONS = [
  { value: 'CLEARED', label: 'Confirmada' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'SCHEDULED', label: 'Agendada' },
  { value: 'CANCELLED', label: 'Cancelada' },
] as const

const schema = z.object({
  tipo: z.enum(['RECEITA', 'DESPESA', 'TRANSFERENCIA']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3).default('BRL'),
  data: z.string().min(1, 'Data obrigatoria'),
  descricao: z.string().min(1, 'Descricao obrigatoria').max(200),
  contaId: z.string().uuid('Selecione uma conta'),
  contaDestinoId: z.string().uuid().optional(),
  categoriaId: z.string().uuid().optional(),
  status: z.enum(['CLEARED', 'PENDING', 'SCHEDULED', 'CANCELLED']).default('CLEARED'),
  payeeId: z.string().uuid().optional(),
  tagIds: z.array(z.string().uuid()).default([]),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
] as const

export default function NovaTransacaoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: contas } = useQuery({
    queryKey: ['contas'],
    queryFn: () => contasService.listar(),
  })

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const { data: payees, isLoading: payeesLoading } = useQuery({
    queryKey: ['payees'],
    queryFn: listarPayees,
  })

  const { data: tags, isLoading: tagsLoading } = useQuery({
    queryKey: ['tags'],
    queryFn: listarTags,
  })

  const today = new Date().toISOString().slice(0, 10)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      tipo: 'DESPESA',
      valor: 0,
      moeda: 'BRL',
      data: today,
      descricao: '',
      contaId: '',
      contaDestinoId: undefined,
      categoriaId: undefined,
      status: 'CLEARED',
      payeeId: undefined,
      tagIds: [],
    },
  })

  // eslint-disable-next-line react-hooks/incompatible-library
  const tipoAtual = form.watch('tipo')
  const tagIdsAtual = form.watch('tagIds')
  const isTransferencia = tipoAtual === 'TRANSFERENCIA'

  // Deduplicar por nome dentro do mesmo tipo (IDs distintos, nomes iguais = dado duplicado no banco)
  const categoriasDoTipo = (categorias ?? [])
    .filter(c => c.tipo === tipoAtual)
    .filter((c, idx, arr) => arr.findIndex(x => x.nome === c.nome) === idx)

  const temDuplicatas = (categorias ?? [])
    .filter(c => c.tipo === tipoAtual).length > categoriasDoTipo.length

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      transacoesService.criar({
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        data: values.data,
        descricao: values.descricao,
        contaId: values.contaId,
        ...(values.contaDestinoId ? { contaDestinoId: values.contaDestinoId } : {}),
        ...(values.categoriaId ? { categoriaId: values.categoriaId } : {}),
        status: values.status,
        ...(values.payeeId ? { payeeId: values.payeeId } : {}),
        ...(values.tagIds.length > 0 ? { tagIds: values.tagIds } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transacoes'] })
      router.push('/transacoes')
    },
    onError: () => {
      setApiError('Erro ao registrar transacao.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Transacao</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })}
                className="space-y-4"
              >
                {/* Tipo */}
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Controller
                    control={form.control}
                    name="tipo"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue>
                            {(v: string | null) => TIPOS.find(t => t.value === v)?.label ?? 'Selecione o tipo'}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {TIPOS.map((t) => (
                            <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                </FormItem>

                {/* Valor */}
                <FormField
                  control={form.control}
                  name="valor"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Valor (R$)</FormLabel>
                      <FormControl>
                        <MoneyInput value={field.value} onChange={field.onChange} id={field.name} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Data */}
                <FormField
                  control={form.control}
                  name="data"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Data</FormLabel>
                      <FormControl>
                        <Input type="date" className="w-full max-w-xs" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Descricao */}
                <FormField
                  control={form.control}
                  name="descricao"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Descricao</FormLabel>
                      <FormControl>
                        <Input className="w-full" placeholder="Ex: Supermercado" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Conta origem */}
                <FormItem>
                  <FormLabel>Conta {isTransferencia ? 'de origem' : ''}</FormLabel>
                  <Controller
                    control={form.control}
                    name="contaId"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="Selecione a conta">
                            {(v: string | null) => {
                              if (!v) return 'Selecione a conta'
                              return (contas ?? []).find(c => c.id === v)?.nome ?? 'Selecione a conta'
                            }}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {(contas ?? []).filter(c => c.ativa).map((c) => (
                            <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {form.formState.errors.contaId && (
                    <p className="text-sm text-destructive">{form.formState.errors.contaId.message}</p>
                  )}
                </FormItem>

                {/* Conta destino (so para transferencia) */}
                {isTransferencia && (
                  <FormItem>
                    <FormLabel>Conta de destino</FormLabel>
                    <Controller
                      control={form.control}
                      name="contaDestinoId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Selecione a conta de destino">
                              {(v: string | null) => {
                                if (!v) return 'Selecione a conta de destino'
                                return (contas ?? []).find(c => c.id === v)?.nome ?? 'Selecione a conta de destino'
                              }}
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {(contas ?? []).filter(c => c.ativa).map((c) => (
                              <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                {/* Categoria (opcional, oculto para transferencia) */}
                {!isTransferencia && categoriasDoTipo.length > 0 && (
                  <FormItem>
                    <FormLabel>Categoria (opcional)</FormLabel>
                    {temDuplicatas && (
                      <p className="text-xs text-amber-600">
                        Categorias com nomes duplicados detectadas. Exibindo uma de cada.
                      </p>
                    )}
                    <Controller
                      control={form.control}
                      name="categoriaId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Sem categoria">
                              {(v: string | null) => {
                                if (!v) return 'Sem categoria'
                                return (categoriasDoTipo ?? []).find(c => c.id === v)?.nome ?? 'Sem categoria'
                              }}
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {categoriasDoTipo.map((c) => (
                              <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                {/* Status */}
                <FormItem>
                  <FormLabel>Status</FormLabel>
                  <Controller
                    control={form.control}
                    name="status"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue>
                            {(v: string | null) => STATUS_OPTIONS.find(s => s.value === v)?.label ?? 'Selecione o status'}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {STATUS_OPTIONS.map((s) => (
                            <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                </FormItem>

                {/* Payee (opcional, exibe somente se houver payees carregados) */}
                {payeesLoading && (
                  <FormItem>
                    <FormLabel>Beneficiario (opcional)</FormLabel>
                    <Skeleton className="h-9 w-full" />
                  </FormItem>
                )}
                {!payeesLoading && (payees ?? []).length > 0 && (
                  <FormItem>
                    <FormLabel>Beneficiario (opcional)</FormLabel>
                    <Controller
                      control={form.control}
                      name="payeeId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Sem beneficiario">
                              {(v: string | null) => {
                                if (!v) return 'Sem beneficiario'
                                return (payees ?? []).find(p => p.id === v)?.nome ?? 'Sem beneficiario'
                              }}
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {(payees ?? []).map((p) => (
                              <SelectItem key={p.id} value={p.id}>{p.nome}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                {/* Tags (multipla selecao via checkboxes, exibe somente se houver tags) */}
                {tagsLoading && (
                  <FormItem>
                    <FormLabel>Tags</FormLabel>
                    <Skeleton className="h-9 w-full" />
                  </FormItem>
                )}
                {!tagsLoading && (tags ?? []).length > 0 && (
                  <FormItem>
                    <FormLabel>Tags</FormLabel>
                    <div className="flex flex-wrap gap-2 pt-1">
                      {(tags ?? []).map((tag) => {
                        const checked = tagIdsAtual.includes(tag.id)
                        return (
                          <label
                            key={tag.id}
                            className="flex items-center gap-1.5 cursor-pointer select-none text-sm"
                          >
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={(e) => {
                                const current = form.getValues('tagIds')
                                if (e.target.checked) {
                                  form.setValue('tagIds', [...current, tag.id])
                                } else {
                                  form.setValue('tagIds', current.filter(id => id !== tag.id))
                                }
                              }}
                              className="accent-primary"
                            />
                            {tag.cor && (
                              <span
                                className="inline-block w-3 h-3 rounded-full"
                                style={{ backgroundColor: tag.cor }}
                                aria-hidden="true"
                              />
                            )}
                            {tag.nome}
                          </label>
                        )
                      })}
                    </div>
                  </FormItem>
                )}

                <input type="hidden" {...form.register('moeda')} />

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button type="button" variant="outline" onClick={() => router.push('/transacoes')}>
                    Cancelar
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
