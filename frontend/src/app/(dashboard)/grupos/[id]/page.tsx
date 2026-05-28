'use client'
import { useState, useEffect } from 'react'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { listarGrupos, atualizarGrupo } from '@/features/grupo'
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
import { Skeleton } from '@/shared/components/ui/skeleton'
import { useDraftForm } from '@/shared/hooks/useDraftForm'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100, 'Maximo 100 caracteres'),
  descricao: z
    .string()
    .max(300, 'Maximo 300 caracteres')
    .optional()
    .or(z.literal('')),
})

type FormValues = z.infer<typeof schema>

export default function EditarGrupoPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: grupos, isLoading } = useQuery({
    queryKey: ['grupos'],
    queryFn: listarGrupos,
  })

  const grupo = grupos?.find((g) => g.id === id)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: { nome: '', descricao: '' },
  })

  const { clearDraft, resetWithDraft } = useDraftForm(form)

  useEffect(() => {
    if (grupo) {
      resetWithDraft({ nome: grupo.nome, descricao: grupo.descricao ?? '' })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [grupo])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      atualizarGrupo(id, {
        nome: values.nome,
        ...(values.descricao && values.descricao.length > 0
          ? { descricao: values.descricao }
          : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['grupos'] })
      clearDraft()
      router.push('/grupos')
    },
    onError: () => {
      setApiError('Erro ao atualizar grupo.')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Skeleton className="h-10 w-10" />
          <Skeleton className="h-8 w-48" />
        </div>
        <div className="max-w-xl">
          <Card>
            <CardContent className="pt-6 space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-20 w-full" />
            </CardContent>
          </Card>
        </div>
      </div>
    )
  }

  if (!isLoading && !grupo) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            aria-label="Voltar"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Grupo</h1>
        </div>
        <p className="text-sm text-destructive">Grupo nao encontrado.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Voltar"
          onClick={() => router.back()}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Grupo</h1>
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
                <FormField
                  control={form.control}
                  name="nome"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome</FormLabel>
                      <FormControl>
                        <Input
                          className="w-full"
                          placeholder="Ex: Viagem Europa, Casa Nova"
                          maxLength={100}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="descricao"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Descricao (opcional)</FormLabel>
                      <FormControl>
                        <textarea
                          className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                          placeholder="Descricao do grupo"
                          maxLength={300}
                          rows={3}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {grupo && !grupo.ativo && (
                  <p className="text-sm text-muted-foreground">
                    Este grupo esta inativo.
                  </p>
                )}

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { clearDraft(); router.push('/grupos') }}
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
