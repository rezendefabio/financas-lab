'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
import type { Anotacao, PrioridadeAnotacao, TipoAnotacao } from '@/features/anotacoes/types/anotacao'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDate } from '@/shared/lib/formatters'

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

function AnotacaoRow({
  anotacao,
  onVer,
  onDeletar,
}: {
  anotacao: Anotacao
  onVer: () => void
  onDeletar: () => void
}) {
  const [confirmando, setConfirmando] = useState(false)
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="py-3 px-4 font-medium">{anotacao.titulo}</td>
      <td className="py-3 px-4">
        <Badge variant="outline">{TIPO_LABELS[anotacao.tipo]}</Badge>
      </td>
      <td className="py-3 px-4">
        <Badge variant={PRIORIDADE_BADGE_VARIANT[anotacao.prioridade]}>
          {PRIORIDADE_LABELS[anotacao.prioridade]}
        </Badge>
      </td>
      <td className="py-3 px-4 tabular-nums text-sm">
        {anotacao.valorMontante != null ? formatBRL(anotacao.valorMontante) : '--'}
      </td>
      <td className="py-3 px-4 text-sm">
        {anotacao.dataReferencia ? formatDate(anotacao.dataReferencia) : '--'}
      </td>
      <td className="py-3 px-4">
        <div className="flex items-center gap-2">
          <Button size="sm" variant="outline" onClick={onVer}>
            Ver
          </Button>
          {!confirmando ? (
            <Button
              size="sm"
              variant="outline"
              className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
              onClick={() => setConfirmando(true)}
            >
              Deletar
            </Button>
          ) : (
            <div className="flex items-center gap-1">
              <Button size="sm" variant="destructive" onClick={onDeletar}>
                Confirmar
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setConfirmando(false)}>
                Cancelar
              </Button>
            </div>
          )}
        </div>
      </td>
    </tr>
  )
}

export default function AnotacoesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['anotacoes'],
    queryFn: anotacaoService.listar,
  })

  const deletarMutation = useMutation({
    mutationFn: (id: string) => anotacaoService.deletar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['anotacoes'] })
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Anotacoes</h1>
        <Button onClick={() => router.push('/anotacoes/novo')}>Nova Anotacao</Button>
      </div>

      {isLoading && (
        <Card>
          <CardContent className="pt-6 space-y-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </CardContent>
        </Card>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar anotacoes.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma anotacao cadastrada.</p>
          <Button onClick={() => router.push('/anotacoes/novo')}>Criar primeira anotacao</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <Card>
          <CardContent className="pt-4 pb-0 px-0">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Titulo</th>
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Tipo</th>
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Prioridade</th>
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Valor</th>
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Data Ref.</th>
                  <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Acoes</th>
                </tr>
              </thead>
              <tbody>
                {data.map((anotacao) => (
                  <AnotacaoRow
                    key={anotacao.id}
                    anotacao={anotacao}
                    onVer={() => router.push(`/anotacoes/${anotacao.id}`)}
                    onDeletar={() => deletarMutation.mutate(anotacao.id)}
                  />
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
