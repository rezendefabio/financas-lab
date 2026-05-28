'use client'
import { useState, useEffect } from 'react'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { buscarFatura, atualizarFatura } from '@/features/fatura'
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

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100, 'Maximo 100 caracteres'),
  dataVencimento: z.string().min(1, 'Data de vencimento obrigatoria'),
  dataFechamento: z.string().optional().or(z.literal('')),
  valorTotalValor: z.coerce.number().optional(),
})

type FormValues = z.infer<typeof schema>

export default function EditarFaturaPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: fatura, isLoading, isError } = useQuery({
    queryKey: ['fatura', id],
    queryFn: () => buscarFatura(id),
  })

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      dataVencimento: '',
      dataFechamento: '',
      valorTotalValor: 0,
    },
  })

  const { clearDraft, resetWithDraft } = useDraftForm(form)

  useEffect(() => {
    if (fatura) {
      resetWithDraft({
        nome: fatura.nome,
        dataVencimento: fatura.dataVencimento,
        dataFechamento: fatura.dataFechamento ?? '',
        valorTotalValor: fatura.valorTotal?.valor ?? 0,
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fatura])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      atualizarFatura(id, {
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
      await queryClient.invalidateQueries({ queryKey: ['fatura', id] })
      clearDraft()
      router.push('/faturas')
    },
    onError: () => {
      setApiError('Erro ao atualizar fatura.')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Fatura</h1>
        </div>
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    )
  }

  if (isError || !fatura) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Fatura</h1>
        </div>
        <p className="text-sm text-destructive">Fatura nao encontrada.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Fatura</h1>
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
