'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { lancamentoRecorrenteService } from '@/features/lancamentorecorrente'
import { contasService } from '@/features/contas/services/contas.service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
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
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { LookupField } from '@/shared/components/LookupField'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const schema = z.object({
  descricao: z.string().min(1, 'Descricao obrigatoria').max(200),
  tipo: z.enum(['RECEITA', 'DESPESA']),
  valorValor: z.coerce.number().positive('Valor deve ser positivo'),
  contaId: z.string().uuid('Selecione uma conta'),
  categoriaId: z.string().uuid().optional(),
  periodicidade: z.enum([
    'SEMANAL',
    'QUINZENAL',
    'MENSAL',
    'BIMESTRAL',
    'TRIMESTRAL',
    'SEMESTRAL',
    'ANUAL',
  ]),
  proximaOcorrencia: z.string().min(1, 'Data obrigatoria'),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
] as const

const PERIODICIDADES = [
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'QUINZENAL', label: 'Quinzenal' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'BIMESTRAL', label: 'Bimestral' },
  { value: 'TRIMESTRAL', label: 'Trimestral' },
  { value: 'SEMESTRAL', label: 'Semestral' },
  { value: 'ANUAL', label: 'Anual' },
] as const

export default function NovoLancamentoRecorrentePage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const today = new Date().toISOString().slice(0, 10)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      descricao: '',
      tipo: 'DESPESA',
      valorValor: 0,
      contaId: '',
      categoriaId: undefined,
      periodicidade: 'MENSAL',
      proximaOcorrencia: today,
    },
  })
  const { clearDraft } = useDraftForm(form)

  // eslint-disable-next-line react-hooks/incompatible-library
  const tipoAtual = form.watch('tipo')

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      lancamentoRecorrenteService.criar({
        descricao: values.descricao,
        tipo: values.tipo,
        valorValor: values.valorValor,
        valorMoeda: 'BRL',
        contaId: values.contaId,
        ...(values.categoriaId ? { categoriaId: values.categoriaId } : {}),
        periodicidade: values.periodicidade,
        proximaOcorrencia: values.proximaOcorrencia,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lancamentos-recorrentes'] })
      clearDraft()
      router.push('/lancamentos-recorrentes')
    },
    onError: () => {
      setApiError('Erro ao criar lancamento recorrente.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Novo Lancamento Recorrente</h1>
      </div>

      <div className="max-w-2xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((v) => {
                  setApiError(null)
                  mutation.mutate(v)
                })}
                className="space-y-4"
              >
                <FormGrid>
                  {/* Descricao */}
                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="descricao"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Descricao</FormLabel>
                          <FormControl>
                            <Input
                              className="w-full"
                              placeholder="Ex: Aluguel"
                              maxLength={200}
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  {/* Valor */}
                  <FormCol span={7}>
                    <FormField
                      control={form.control}
                      name="valorValor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Valor (R$)</FormLabel>
                          <FormControl>
                            <MoneyInput
                              value={field.value}
                              onChange={field.onChange}
                              id={field.name}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  {/* Tipo */}
                  <FormCol span={5}>
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
                                <SelectItem key={t.value} value={t.value}>
                                  {t.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                    </FormItem>
                  </FormCol>

                  {/* Periodicidade */}
                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Periodicidade</FormLabel>
                      <Controller
                        control={form.control}
                        name="periodicidade"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full">
                              <SelectValue>
                                {(v: string | null) => PERIODICIDADES.find(p => p.value === v)?.label ?? 'Selecione a periodicidade'}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {PERIODICIDADES.map((p) => (
                                <SelectItem key={p.value} value={p.value}>
                                  {p.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                    </FormItem>
                  </FormCol>

                  {/* Proxima Ocorrencia */}
                  <FormCol span={6}>
                    <FormField
                      control={form.control}
                      name="proximaOcorrencia"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Proxima Ocorrencia</FormLabel>
                          <FormControl>
                            <Input type="date" className="w-full" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  {/* Conta */}
                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Conta</FormLabel>
                      <Controller
                        control={form.control}
                        name="contaId"
                        render={({ field }) => (
                          <LookupField
                            value={field.value || null}
                            onChange={(v) => field.onChange(v ?? '')}
                            queryKey={['contas', 'ativas']}
                            queryFn={() =>
                              contasService
                                .listar()
                                .then((cs) =>
                                  cs
                                    .filter((c) => c.ativa)
                                    .map((c) => ({ value: c.id, label: c.nome })),
                                )
                            }
                            placeholder="Selecione a conta"
                          />
                        )}
                      />
                      {form.formState.errors.contaId && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.contaId.message}
                        </p>
                      )}
                    </FormItem>
                  </FormCol>

                  {/* Categoria (opcional) */}
                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Categoria (opcional)</FormLabel>
                      <Controller
                        control={form.control}
                        name="categoriaId"
                        render={({ field }) => (
                          <LookupField
                            value={field.value ?? null}
                            onChange={(v) => field.onChange(v ?? undefined)}
                            queryKey={['categorias', tipoAtual]}
                            queryFn={() =>
                              categoriasService.listar().then((cats) =>
                                cats
                                  .filter((c) => c.tipo === tipoAtual)
                                  .map((c) => ({ value: c.id, label: c.nome })),
                              )
                            }
                            placeholder="Sem categoria"
                            emptyMessage="Nenhuma categoria para este tipo."
                          />
                        )}
                      />
                    </FormItem>
                  </FormCol>
                </FormGrid>

                {apiError && <p className="text-sm text-destructive">{apiError}</p>}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { clearDraft(); router.push('/lancamentos-recorrentes') }}
                  >
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
