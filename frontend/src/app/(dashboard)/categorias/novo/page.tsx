'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
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
  nome: z.string().min(1, 'Nome obrigatorio').max(100),
  tipo: z.enum(['RECEITA', 'DESPESA']),
  categoriaPaiId: z.string().uuid().optional(),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
] as const

export default function NovaCategoriaPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      tipo: 'DESPESA',
      categoriaPaiId: undefined,
    },
  })

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      categoriasService.criar({
        nome: values.nome,
        tipo: values.tipo,
        ...(values.categoriaPaiId ? { categoriaPaiId: values.categoriaPaiId } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['categorias'] })
      router.push('/categorias')
    },
    onError: () => {
      setApiError('Erro ao criar categoria.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Categoria</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })} className="space-y-4">
                <FormGrid>
                  <FormCol span={8}>
                    <FormField
                      control={form.control}
                      name="nome"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Nome</FormLabel>
                          <FormControl>
                            <Input className="w-full" placeholder="Ex: Alimentacao" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={4}>
                    <FormItem>
                      <FormLabel>Tipo</FormLabel>
                      <Controller
                        control={form.control}
                        name="tipo"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Selecione o tipo">
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
                      {form.formState.errors.tipo && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.tipo.message}
                        </p>
                      )}
                    </FormItem>
                  </FormCol>

                  <FormCol span={12}>
                    <FormItem>
                      <FormLabel>Categoria pai (opcional)</FormLabel>
                      <Controller
                        control={form.control}
                        name="categoriaPaiId"
                        render={({ field }) => (
                          <LookupField
                            value={field.value ?? null}
                            onChange={(v) => field.onChange(v ?? undefined)}
                            queryKey={['categorias', 'lookup']}
                            queryFn={() =>
                              categoriasService
                                .listar()
                                .then((cs) => cs.map((c) => ({ value: c.id, label: c.nome })))
                            }
                            placeholder="Nenhuma (categoria raiz)"
                          />
                        )}
                      />
                    </FormItem>
                  </FormCol>
                </FormGrid>

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button type="button" variant="outline" onClick={() => router.push('/categorias')}>
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
