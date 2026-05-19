'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
import type {
  Anotacao,
  PrioridadeAnotacao,
  TipoAnotacao,
} from '@/features/anotacoes/types/anotacao'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { exportToCsv } from '@/shared/lib/export-csv'
import { formatBRL, formatDate } from '@/shared/lib/formatters'

const SCREEN_CODE = 'FIN-ANO-001'

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

const PRIORIDADE_BADGE_VARIANT: Record<
  PrioridadeAnotacao,
  'default' | 'secondary' | 'destructive' | 'outline'
> = {
  BAIXA: 'secondary',
  MEDIA: 'outline',
  ALTA: 'default',
  URGENTE: 'destructive',
}

function AnotacaoActions({
  anotacao,
  onVer,
  onLog,
  onDeletar,
}: {
  anotacao: Anotacao
  onVer: () => void
  onLog: () => void
  onDeletar: () => void
}) {
  const [confirmando, setConfirmando] = useState(false)
  return (
    <div className="flex items-center justify-end gap-2">
      <Button
        size="sm"
        variant="ghost"
        aria-label={`Historico de ${anotacao.titulo}`}
        onClick={onLog}
      >
        Log
      </Button>
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
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setConfirmando(false)}
          >
            Cancelar
          </Button>
        </div>
      )}
    </div>
  )
}

export default function AnotacoesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionada, setSelecionada] = useState<Anotacao | null>(null)

  const { data: anotacoes = [], isLoading, isError } = useQuery({
    queryKey: ['anotacoes'],
    queryFn: anotacaoService.listar,
  })

  const deletarMutation = useMutation({
    mutationFn: (id: string) => anotacaoService.deletar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['anotacoes'] })
    },
  })

  const columns: ColumnDef<Anotacao>[] = [
    {
      key: 'titulo',
      label: 'Titulo',
      sortable: true,
      className: 'font-medium',
    },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (value) => (
        <Badge variant="outline">
          {TIPO_LABELS[value as TipoAnotacao] ?? String(value)}
        </Badge>
      ),
    },
    {
      key: 'prioridade',
      label: 'Prioridade',
      render: (value) => (
        <Badge variant={PRIORIDADE_BADGE_VARIANT[value as PrioridadeAnotacao]}>
          {PRIORIDADE_LABELS[value as PrioridadeAnotacao] ?? String(value)}
        </Badge>
      ),
    },
    {
      key: 'valorMontante',
      label: 'Valor',
      className: 'text-right tabular-nums',
      render: (value) => (value != null ? formatBRL(Number(value)) : '--'),
    },
    {
      key: 'dataReferencia',
      label: 'Data Ref.',
      render: (value) => (value ? formatDate(String(value)) : '--'),
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'anotacoes',
      anotacoes.map((a) => ({
        titulo: a.titulo,
        tipo: TIPO_LABELS[a.tipo] ?? a.tipo,
        prioridade: PRIORIDADE_LABELS[a.prioridade] ?? a.prioridade,
        valor: a.valorMontante ?? '',
        dataReferencia: a.dataReferencia ?? '',
      })),
      [
        { key: 'titulo', label: 'Titulo' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'prioridade', label: 'Prioridade' },
        { key: 'valor', label: 'Valor' },
        { key: 'dataReferencia', label: 'Data Ref.' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Anotacoes</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="anotacao"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.titulo}
            screenCode={SCREEN_CODE}
            onExportCsv={anotacoes.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/anotacoes/novo')}>
            Nova Anotacao
          </Button>
        </div>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar anotacoes.</p>
      )}

      {!isError && !isLoading && anotacoes.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma anotacao cadastrada.</p>
          <Button onClick={() => router.push('/anotacoes/novo')}>
            Criar primeira anotacao
          </Button>
        </div>
      )}

      {!isError && (isLoading || anotacoes.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={anotacoes}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma anotacao encontrada."
              rowActions={(anotacao) => (
                <AnotacaoActions
                  anotacao={anotacao}
                  onVer={() => router.push(`/anotacoes/${anotacao.id}`)}
                  onLog={() => setSelecionada(anotacao)}
                  onDeletar={() => deletarMutation.mutate(anotacao.id)}
                />
              )}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
