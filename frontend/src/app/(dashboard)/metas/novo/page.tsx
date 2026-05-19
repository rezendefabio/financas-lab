'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { metaService } from '@/features/metas/services/meta-service'
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

const hoje = new Date().toISOString().split('T')[0]

const schema = z.object({
  nome: z.string().min(1, 'Nome e obrigatorio'),
  valorAlvoValor: z.coerce.number().positive('Valor deve ser positivo'),
  prazo: z
    .string()
    .min(1, 'Prazo e obrigatorio')
    .refine((val) => val >= hoje, 'Data deve ser hoje ou futura'),
})

type FormValues = z.infer<typeof schema>

export default function NovaMetaPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      valorAlvoValor: 0,
      prazo: '',
    },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      metaService.criar({
        nome: values.nome,
        valorAlvoValor: values.valorAlvoValor,
        valorAlvoMoeda: 'BRL',
        prazo: values.prazo,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['metas'] })
      router.push('/metas')
    },
    onError: () => {
      setApiError('Erro ao criar meta.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Meta</h1>
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
                  {/* Nome */}
                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="nome"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Nome</FormLabel>
                          <FormControl>
                            <Input type="text" className="w-full" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  {/* Valor Alvo */}
                  <FormCol span={7}>
                    <FormField
                      control={form.control}
                      name="valorAlvoValor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Valor Alvo (R$)</FormLabel>
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

                  {/* Prazo */}
                  <FormCol span={5}>
                    <FormField
                      control={form.control}
                      name="prazo"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Prazo</FormLabel>
                          <FormControl>
                            <Input type="date" className="w-full" {...field} />
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
                    onClick={() => router.push('/metas')}
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
