'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
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
import { MoneyInput } from '@/shared/components/MoneyInput'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const schema = z.object({
  titulo: z.string().min(1, 'Titulo obrigatorio').max(200),
  conteudo: z.string().max(5000).optional(),
  tipo: z.enum(['LEMBRETE', 'OBSERVACAO', 'ALERTA', 'PLANEJAMENTO']),
  prioridade: z.enum(['BAIXA', 'MEDIA', 'ALTA', 'URGENTE']),
  valorMontante: z.coerce.number().min(0).optional(),
  dataReferencia: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'LEMBRETE', label: 'Lembrete' },
  { value: 'OBSERVACAO', label: 'Observacao' },
  { value: 'ALERTA', label: 'Alerta' },
  { value: 'PLANEJAMENTO', label: 'Planejamento' },
] as const

const PRIORIDADES = [
  { value: 'BAIXA', label: 'Baixa' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'ALTA', label: 'Alta' },
  { value: 'URGENTE', label: 'Urgente' },
] as const

export default function NovaAnotacao() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      titulo: '',
      conteudo: '',
      tipo: 'LEMBRETE',
      prioridade: 'MEDIA',
      valorMontante: 0,
      dataReferencia: '',
    },
  })
  const { clearDraft } = useDraftForm(form)

  const mutation = useMutation({
    mutationFn: anotacaoService.criar,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['anotacoes'] })
      clearDraft()
      router.push('/anotacoes')
    },
    onError: () => {
      setApiError('Erro ao criar anotacao.')
    },
  })

  async function onSubmit(values: FormValues) {
    setApiError(null)
    mutation.mutate({
      titulo: values.titulo,
      conteudo: values.conteudo || undefined,
      tipo: values.tipo,
      prioridade: values.prioridade,
      valorMontante: values.valorMontante && values.valorMontante > 0 ? values.valorMontante : undefined,
      valorMoeda: values.valorMontante && values.valorMontante > 0 ? 'BRL' : undefined,
      dataReferencia: values.dataReferencia || undefined,
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Anotacao</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormGrid>
                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="titulo"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Titulo</FormLabel>
                          <FormControl>
                            <Input maxLength={200} className="w-full" placeholder="Ex: Pagar fatura" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={12}>
                    <FormField
                      control={form.control}
                      name="conteudo"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Conteudo (opcional)</FormLabel>
                          <FormControl>
                            <textarea
                              maxLength={5000}
                              className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                              placeholder="Detalhes..."
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
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Selecione o tipo">
                                {(v: string | null) => TIPOS.find(t => t.value === v)?.label ?? 'Selecione o tipo'}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {TIPOS.map((t) => (
                                <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                      {form.formState.errors.tipo && (
                        <p className="text-sm text-destructive">{form.formState.errors.tipo.message}</p>
                      )}
                    </FormItem>
                  </FormCol>

                  <FormCol span={6}>
                    <FormItem>
                      <FormLabel>Prioridade</FormLabel>
                      <Controller
                        control={form.control}
                        name="prioridade"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Selecione a prioridade">
                                {(v: string | null) => PRIORIDADES.find(p => p.value === v)?.label ?? 'Selecione a prioridade'}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {PRIORIDADES.map((p) => (
                                <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                      {form.formState.errors.prioridade && (
                        <p className="text-sm text-destructive">{form.formState.errors.prioridade.message}</p>
                      )}
                    </FormItem>
                  </FormCol>

                  <FormCol span={6}>
                    <FormField
                      control={form.control}
                      name="valorMontante"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Valor (opcional, R$)</FormLabel>
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

                  <FormCol span={6}>
                    <FormField
                      control={form.control}
                      name="dataReferencia"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Data de referencia (opcional)</FormLabel>
                          <FormControl>
                            <Input type="date" className="w-full" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>
                </FormGrid>

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
                    onClick={() => { clearDraft(); router.push('/anotacoes') }}
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
