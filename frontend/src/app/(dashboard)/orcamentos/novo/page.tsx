'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

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

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      categoriaId: '',
      valorLimiteValor: 0,
      mesAno: '',
    },
  })

  // Agrupado: DESPESA primeiro, depois RECEITA
  const categoriasDespesa = (categorias ?? []).filter((c) => c.tipo === 'DESPESA')
  const categoriasReceita = (categorias ?? []).filter((c) => c.tipo === 'RECEITA')
  const categoriasOrdenadas = [...categoriasDespesa, ...categoriasReceita]

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
                {/* Categoria */}
                <FormField
                  control={form.control}
                  name="categoriaId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Categoria</FormLabel>
                      <FormControl>
                        <Select value={field.value} onValueChange={field.onChange}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Selecione uma categoria" />
                          </SelectTrigger>
                          <SelectContent>
                            {categoriasDespesa.length > 0 && (
                              <>
                                <div className="px-2 py-1 text-xs font-semibold text-muted-foreground">
                                  Despesa
                                </div>
                                {categoriasDespesa.map((c) => (
                                  <SelectItem key={c.id} value={c.id}>
                                    {c.nome}
                                  </SelectItem>
                                ))}
                              </>
                            )}
                            {categoriasReceita.length > 0 && (
                              <>
                                <div className="px-2 py-1 text-xs font-semibold text-muted-foreground">
                                  Receita
                                </div>
                                {categoriasReceita.map((c) => (
                                  <SelectItem key={c.id} value={c.id}>
                                    {c.nome}
                                  </SelectItem>
                                ))}
                              </>
                            )}
                            {categoriasOrdenadas.length === 0 && (
                              <div className="px-2 py-2 text-sm text-muted-foreground">
                                Nenhuma categoria disponivel
                              </div>
                            )}
                          </SelectContent>
                        </Select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Valor Limite */}
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

                {/* Mes/Ano */}
                <FormField
                  control={form.control}
                  name="mesAno"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mes/Ano</FormLabel>
                      <FormControl>
                        <Input type="month" className="w-full max-w-xs" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {apiError && <p className="text-sm text-destructive">{apiError}</p>}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => router.push('/orcamentos')}
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
