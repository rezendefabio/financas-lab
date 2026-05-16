'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
import type { PrioridadeAnotacao, TipoAnotacao } from '@/features/anotacoes/types/anotacao'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDate, formatDateTime } from '@/shared/lib/formatters'

const TIPO_LABELS: Record<TipoAnotacao, string> = {
  LEMBRETE: 'Lembrete',
  OBSERVACAO: 'Observacao',
  ALERTA: 'Alerta',
  PLANEJAMENTO: 'Planejamento',
}

const PRIORIDADE_LABELS: Record<PrioridadeAnotacao, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Media',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
}

const PRIORIDADE_BADGE_VARIANT: Record<PrioridadeAnotacao, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  BAIXA: 'secondary',
  MEDIA: 'outline',
  ALTA: 'default',
  URGENTE: 'destructive',
}

export default function AnotacaoDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmando, setConfirmando] = useState(false)

  const { data: anotacao, isLoading, isError } = useQuery({
    queryKey: ['anotacao', id],
    queryFn: () => anotacaoService.buscarPorId(id),
  })

  const deletarMutation = useMutation({
    mutationFn: () => anotacaoService.deletar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['anotacoes'] })
      router.push('/anotacoes')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Card>
          <CardContent className="space-y-4 pt-6">
            <Skeleton className="h-5 w-32" />
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-8 w-36" />
          </CardContent>
        </Card>
      </div>
    )
  }

  if (isError || !anotacao) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-destructive">Anotacao nao encontrada.</p>
        <Button variant="outline" onClick={() => router.push('/anotacoes')}>Voltar</Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">{anotacao.titulo}</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Detalhes</CardTitle>
              <Badge variant={PRIORIDADE_BADGE_VARIANT[anotacao.prioridade]}>
                {PRIORIDADE_LABELS[anotacao.prioridade]}
              </Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-sm text-muted-foreground">Titulo</dt>
                <dd className="font-medium">{anotacao.titulo}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Tipo</dt>
                <dd>
                  <Badge variant="outline">{TIPO_LABELS[anotacao.tipo]}</Badge>
                </dd>
              </div>
              {anotacao.conteudo && (
                <div className="sm:col-span-2">
                  <dt className="text-sm text-muted-foreground">Conteudo</dt>
                  <dd className="whitespace-pre-wrap">{anotacao.conteudo}</dd>
                </div>
              )}
              {anotacao.valorMontante != null && (
                <div>
                  <dt className="text-sm text-muted-foreground">Valor</dt>
                  <dd className="tabular-nums font-semibold">{formatBRL(anotacao.valorMontante)}</dd>
                </div>
              )}
              {anotacao.dataReferencia && (
                <div>
                  <dt className="text-sm text-muted-foreground">Data de referencia</dt>
                  <dd>{formatDate(anotacao.dataReferencia)}</dd>
                </div>
              )}
              <div>
                <dt className="text-sm text-muted-foreground">Criado em</dt>
                <dd className="text-sm">{formatDateTime(anotacao.criadoEm)}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Atualizado em</dt>
                <dd className="text-sm">{formatDateTime(anotacao.atualizadoEm)}</dd>
              </div>
            </dl>

            <div className="flex gap-3 pt-2 border-t">
              <Button onClick={() => router.push(`/anotacoes/${id}/editar`)}>
                Editar
              </Button>
              {!confirmando ? (
                <Button
                  variant="outline"
                  className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
                  onClick={() => setConfirmando(true)}
                >
                  Deletar
                </Button>
              ) : (
                <div className="flex gap-2 items-center">
                  <p className="text-sm text-muted-foreground">Esta acao nao pode ser desfeita. Confirmar?</p>
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={deletarMutation.isPending}
                    onClick={() => deletarMutation.mutate()}
                  >
                    {deletarMutation.isPending ? 'Deletando...' : 'Confirmar'}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setConfirmando(false)}
                  >
                    Cancelar
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
