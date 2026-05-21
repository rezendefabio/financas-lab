'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
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

const schema = z.object({
  categoriaId: z.string().uuid({ message: 'Selecione uma categoria' }),
  valorLimiteValor: z.coerce.number().positive('Valor deve ser positivo'),
  mesAno: z.string().min(1, 'Selecione o mes/ano'),
})

type FormValues = z.infer<typeof schema>

export default function NovoOrcamentoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      categoriaId: '',
      valorLimiteValor: 0,
      mesAno: '',
    },
  })
  const { clearDraft } = useDraftForm(form)

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      orcamentoService.criar({
        categoriaId: values.categoriaId,
        valorLimiteValor: values.valorLimiteValor,
        valorLimiteMoeda: 'BRL',
        mesAno: values.mesAno + '-01',
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orcamentos'] })
      clearDraft()
      router.push('/orcamentos')
    },
    onError: () => {
      setApiError('Erro ao criar orcamento.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Novo Orcamento</h1>
      </div>

      <div className="max-w-xl">
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
                  {/* Categoria */}
                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="categoriaId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Categoria</FormLabel>
                          <FormControl>
                            <LookupField
                              value={field.value || null}
                              onChange={(v) => field.onChange(v ?? '')}
                              queryKey={['categorias', 'lookup']}
                              queryFn={() =>
                                categoriasService
                                  .listar()
                                  .then((cs) => cs.map((c) => ({ value: c.id, label: c.nome })))
                              }
                              placeholder="Selecione uma categoria"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  {/* Valor Limite */}
                  <FormCol span={7}>
                    <FormField
                      control={form.control}
                      name="valorLimiteValor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Valor Limite (R$)</FormLabel>
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

                  <FormCol span={2}>
                    <FormItem>
                      <FormLabel>Moeda</FormLabel>
                      <FormControl>
                        <Input className="w-full" value="BRL" readOnly aria-readonly />
                      </FormControl>
                    </FormItem>
                  </FormCol>

                  {/* Mes/Ano */}
                  <FormCol span={3}>
                    <FormField
                      control={form.control}
                      name="mesAno"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Mes/Ano</FormLabel>
                          <FormControl>
                            <Input type="month" className="w-full" {...field} />
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
                    onClick={() => { clearDraft(); router.push('/orcamentos') }}
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
