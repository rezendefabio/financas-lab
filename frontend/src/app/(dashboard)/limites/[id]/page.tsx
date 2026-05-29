'use client'
import { useState, useEffect } from 'react'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { buscarLimite, atualizarLimite } from '@/features/limite'
import { TIPO_LIMITE_OPTIONS } from '@/features/limite/types/tipo-limite'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100, 'Maximo 100 caracteres'),
  tipo: z.enum(['DIARIO', 'SEMANAL', 'MENSAL', 'ANUAL'], {
    message: 'Selecione um tipo',
  }),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
})

type FormValues = z.infer<typeof schema>

export default function EditarLimitePage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: limite, isLoading, isError } = useQuery({
    queryKey: ['limite', id],
    queryFn: () => buscarLimite(id),
  })

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      tipo: 'MENSAL',
      valor: 0,
    },
  })

  const { clearDraft, resetWithDraft } = useDraftForm(form)

  useEffect(() => {
    if (limite) {
      resetWithDraft({
        nome: limite.nome,
        tipo: limite.tipo,
        valor: limite.valor,
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [limite])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      atualizarLimite(id, {
        nome: values.nome,
        tipo: values.tipo,
        valor: values.valor,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['limites'] })
      await queryClient.invalidateQueries({ queryKey: ['limite', id] })
      clearDraft()
      router.push('/limites')
    },
    onError: () => {
      setApiError('Erro ao atualizar limite.')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Limite</h1>
        </div>
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    )
  }

  if (isError || !limite) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Limite</h1>
        </div>
        <p className="text-sm text-destructive">Limite nao encontrado.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Limite</h1>
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
                              placeholder="Ex: Limite de lazer"
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
                      <FormLabel>Tipo</FormLabel>
                      <Controller
                        control={form.control}
                        name="tipo"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full" aria-label="Tipo">
                              <SelectValue placeholder="Selecione o tipo">
                                {(v: string | null) =>
                                  TIPO_LIMITE_OPTIONS.find((o) => o.value === v)?.label ??
                                  'Selecione o tipo'
                                }
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {TIPO_LIMITE_OPTIONS.map((o) => (
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

                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Valor</FormLabel>
                      <Controller
                        control={form.control}
                        name="valor"
                        render={({ field }) => (
                          <MoneyInput
                            value={field.value}
                            onChange={field.onChange}
                            aria-label="Valor"
                          />
                        )}
                      />
                      {form.formState.errors.valor && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.valor.message}
                        </p>
                      )}
                    </FormItem>
                  </FormCol>
                </FormGrid>

                {!limite.ativo && (
                  <p className="text-sm text-muted-foreground">
                    Este limite esta inativo.
                  </p>
                )}

                {apiError && <p className="text-sm text-destructive">{apiError}</p>}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { clearDraft(); router.push('/limites') }}
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
