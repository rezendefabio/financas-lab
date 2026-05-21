'use client'
import { useEffect, useState } from 'react'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { listarPayees, atualizarPayee } from '@/features/payee/services/payee-service'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
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

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100),
  categoriaPadraoId: z.string().uuid().optional(),
})

type FormValues = z.infer<typeof schema>

export default function EditarPayeePage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: payees, isLoading: payeesLoading, isError: payeesError } = useQuery({
    queryKey: ['payees'],
    queryFn: listarPayees,
  })

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const payee = payees?.find((p) => p.id === id)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: { nome: '', categoriaPadraoId: undefined },
  })
  const { clearDraft, resetWithDraft } = useDraftForm(form)

  useEffect(() => {
    if (payee) {
      resetWithDraft({
        nome: payee.nome,
        categoriaPadraoId: payee.categoriaPadraoId ?? undefined,
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [payee])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      atualizarPayee(id, {
        nome: values.nome,
        categoriaPadraoId: values.categoriaPadraoId,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payees'] })
      clearDraft()
      router.push('/payees')
    },
    onError: () => {
      setApiError('Erro ao atualizar beneficiario.')
    },
  })

  if (payeesLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Beneficiario</h1>
        </div>
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    )
  }

  if (payeesError || (!payeesLoading && !payee)) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Beneficiario</h1>
        </div>
        <p className="text-sm text-destructive">Beneficiario nao encontrado.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Beneficiario</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })}
                className="space-y-4"
              >
                <FormGrid>
                  <FormCol span={7}>
                    <FormField
                      control={form.control}
                      name="nome"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Nome</FormLabel>
                          <FormControl>
                            <Input
                              className="w-full"
                              placeholder="Ex: Supermercado Pao de Acucar"
                              maxLength={100}
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={5}>
                    <FormItem>
                      <FormLabel>Categoria Padrao (opcional)</FormLabel>
                      <Controller
                        control={form.control}
                        name="categoriaPadraoId"
                        render={({ field }) => (
                          <Select
                            value={field.value ?? ''}
                            onValueChange={(v) => field.onChange(v || undefined)}
                          >
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Sem categoria padrao">
                                {(v: string | null) => {
                                  if (!v) return 'Sem categoria padrao'
                                  return categorias?.find(c => c.id === v)?.nome ?? 'Sem categoria padrao'
                                }}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {categorias?.map((c) => (
                                <SelectItem key={c.id} value={c.id}>
                                  {c.nome}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
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
                  <Button type="button" variant="outline" onClick={() => { clearDraft(); router.push('/payees') }}>
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
