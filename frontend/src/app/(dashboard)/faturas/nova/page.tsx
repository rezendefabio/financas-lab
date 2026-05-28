'use client'
import { useState } from 'react'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { criarFatura } from '@/features/fatura'
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
import { MoneyInput } from '@/shared/components/MoneyInput'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { LookupField } from '@/shared/components/LookupField'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100, 'Maximo 100 caracteres'),
  contaId: z.string().uuid('Selecione uma conta'),
  dataVencimento: z.string().min(1, 'Data de vencimento obrigatoria'),
  dataFechamento: z.string().optional().or(z.literal('')),
  valorTotalValor: z.coerce.number().optional(),
})

type FormValues = z.infer<typeof schema>

export default function NovaFaturaPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const today = new Date().toISOString().slice(0, 10)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      contaId: '',
      dataVencimento: today,
      dataFechamento: '',
      valorTotalValor: 0,
    },
  })
  const { clearDraft } = useDraftForm(form)

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      criarFatura({
        contaId: values.contaId,
        nome: values.nome,
        dataVencimento: values.dataVencimento,
        ...(values.dataFechamento && values.dataFechamento.length > 0
          ? { dataFechamento: values.dataFechamento }
          : {}),
        ...(values.valorTotalValor && values.valorTotalValor > 0
          ? { valorTotalValor: values.valorTotalValor, valorTotalMoeda: 'BRL' }
          : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['faturas'] })
      clearDraft()
      router.push('/faturas')
    },
    onError: () => {
      setApiError('Erro ao criar fatura.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Fatura</h1>
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
                              placeholder="Ex: Cartao Maio"
                              maxLength={100}
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={12}>
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
                    <FormField
                      control={form.control}
                      name="dataVencimento"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Data de Vencimento</FormLabel>
                          <FormControl>
                            <Input type="date" className="w-full" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={6}>
                    <FormField
                      control={form.control}
                      name="dataFechamento"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Data de Fechamento (opcional)</FormLabel>
                          <FormControl>
                            <Input type="date" className="w-full" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={6}>
                    <FormField
                      control={form.control}
                      name="valorTotalValor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Valor Total (opcional)</FormLabel>
                          <FormControl>
                            <MoneyInput
                              value={field.value ?? 0}
                              onChange={field.onChange}
                              id={field.name}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
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
                    onClick={() => { clearDraft(); router.push('/faturas') }}
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
