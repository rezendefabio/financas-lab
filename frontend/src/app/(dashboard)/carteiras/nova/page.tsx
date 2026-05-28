'use client'
import { useState } from 'react'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { criarCarteira } from '@/features/carteira'
import { TIPO_CARTEIRA_OPTIONS } from '@/features/carteira/types/tipo-carteira'
import { contasService } from '@/features/contas/services/contas.service'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { LookupField } from '@/shared/components/LookupField'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100, 'Maximo 100 caracteres'),
  contaId: z.string().uuid('Selecione uma conta'),
  tipo: z.enum(['RENDA_FIXA', 'RENDA_VARIAVEL', 'CRIPTOMOEDA', 'OUTROS'], {
    message: 'Selecione um tipo',
  }),
})

type FormValues = z.infer<typeof schema>

export default function NovaCarteiraPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      contaId: '',
      tipo: 'RENDA_FIXA',
    },
  })
  const { clearDraft } = useDraftForm(form)

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      criarCarteira({
        contaId: values.contaId,
        nome: values.nome,
        tipo: values.tipo,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['carteiras'] })
      clearDraft()
      router.push('/carteiras')
    },
    onError: () => {
      setApiError('Erro ao criar carteira.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Carteira</h1>
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
                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="nome"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Nome</FormLabel>
                          <FormControl>
                            <Input
                              className="w-full"
                              placeholder="Ex: Tesouro Direto"
                              maxLength={100}
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

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
                            queryKey={['contas', 'lookup']}
                            queryFn={() =>
                              contasService
                                .listar()
                                .then((cs) =>
                                  cs.map((c) => ({ value: c.id, label: c.nome })),
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

                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Tipo</FormLabel>
                      <Controller
                        control={form.control}
                        name="tipo"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full" aria-label="Tipo">
                              <SelectValue placeholder="Selecione o tipo">
                                {(v: string | null) =>
                                  TIPO_CARTEIRA_OPTIONS.find((o) => o.value === v)?.label ??
                                  'Selecione o tipo'
                                }
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {TIPO_CARTEIRA_OPTIONS.map((o) => (
                                <SelectItem key={o.value} value={o.value}>
                                  {o.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                      {form.formState.errors.tipo && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.tipo.message}
                        </p>
                      )}
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
                    onClick={() => { clearDraft(); router.push('/carteiras') }}
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
