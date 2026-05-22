'use client'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { metaService } from '@/features/metas/services/meta-service'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Progress } from '@/shared/components/ui/progress'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { formatBRL, formatDate, formatDateTime } from '@/shared/lib/formatters'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import type { StatusMeta } from '@/features/metas/types/meta'

function statusVariant(status: StatusMeta): 'default' | 'secondary' | 'outline' | 'destructive' {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'default'
    case 'CONCLUIDA':
      return 'secondary'
    case 'CANCELADA':
      return 'outline'
    default:
      return 'default'
  }
}

function statusLabel(status: StatusMeta): string {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'Em Andamento'
    case 'CONCLUIDA':
      return 'Concluida'
    case 'CANCELADA':
      return 'Cancelada'
    default:
      return status
  }
}

const depositoSchema = z.object({
  valor: z.coerce.number().positive('Valor deve ser positivo'),
})

type DepositoFormValues = z.infer<typeof depositoSchema>

export default function MetaDetalhePage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [cancelarError, setCancelarError] = useState<string | null>(null)
  const [depositoError, setDepositoError] = useState<string | null>(null)

  const { data: meta, isLoading, isError } = useQuery({
    queryKey: ['meta', id],
    queryFn: () => metaService.buscar(id),
  })

  const cancelarMutation = useMutation({
    mutationFn: () => metaService.cancelar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['metas'] })
      router.push('/metas')
    },
    onError: () => {
      setCancelarError('Erro ao cancelar meta.')
    },
  })

  const depositoForm = useForm<DepositoFormValues>({
    resolver: zodResolver(depositoSchema) as Resolver<DepositoFormValues>,
    defaultValues: { valor: 0 },
  })

  const { clearDraft } = useDraftForm(depositoForm)

  const depositoMutation = useMutation({
    mutationFn: (values: DepositoFormValues) =>
      metaService.registrarDeposito(id, { valor: values.valor, moeda: 'BRL' }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['meta', id] })
      clearDraft()
      depositoForm.reset()
    },
    onError: () => {
      setDepositoError('Erro ao registrar deposito.')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !meta) {
    return <p className="text-sm text-destructive">Erro ao carregar meta.</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/metas')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Detalhe da Meta</h1>
      </div>

      {/* Secao 1 - Dados da Meta */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados da Meta</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Nome</p>
              <p className="font-medium">{meta.nome}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Prazo</p>
              <p className="font-medium">{formatDate(meta.prazo)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Valor Alvo</p>
              <p className="font-medium tabular-nums">{formatBRL(meta.valorAlvo.valor)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Criado em</p>
              <p className="font-medium">{formatDateTime(meta.criadoEm)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Status</p>
              <div className="flex items-center gap-2">
                <Badge variant={statusVariant(meta.status)}>{statusLabel(meta.status)}</Badge>
                {meta.atrasada && (
                  <Badge variant="destructive">Atrasada</Badge>
                )}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Secao 2 - Progresso */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Progresso</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Progress value={Math.min(meta.percentualConcluido, 100)} />
          <p className="text-sm text-muted-foreground">
            {meta.percentualConcluido.toFixed(1)}% concluido
          </p>
          <p className="text-sm tabular-nums">
            {formatBRL(meta.valorAtual.valor)} de {formatBRL(meta.valorAlvo.valor)}
          </p>
        </CardContent>
      </Card>

      {/* Secao 3 - Registrar Deposito (somente se EM_ANDAMENTO) */}
      {meta.status === 'EM_ANDAMENTO' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Registrar Deposito</CardTitle>
          </CardHeader>
          <CardContent>
            <Form {...depositoForm}>
              <form
                onSubmit={depositoForm.handleSubmit((v) => {
                  setDepositoError(null)
                  depositoMutation.mutate(v)
                })}
                className="space-y-4"
              >
                <FormField
                  control={depositoForm.control}
                  name="valor"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Valor (R$)</FormLabel>
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

                {depositoError && (
                  <p className="text-sm text-destructive">{depositoError}</p>
                )}

                <Button type="submit" disabled={depositoMutation.isPending}>
                  {depositoMutation.isPending ? 'Registrando...' : 'Registrar'}
                </Button>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      {cancelarError && <p className="text-sm text-destructive">{cancelarError}</p>}

      {meta.status === 'EM_ANDAMENTO' && (
        <Button
          variant="destructive"
          disabled={cancelarMutation.isPending}
          onClick={() => {
            setCancelarError(null)
            cancelarMutation.mutate()
          }}
        >
          {cancelarMutation.isPending ? 'Cancelando...' : 'Cancelar Meta'}
        </Button>
      )}
    </div>
  )
}
