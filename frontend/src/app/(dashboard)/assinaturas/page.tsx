'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assinaturaService } from '@/features/assinaturas/services/assinatura-service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { StatusBadge, CONTA_ATIVA_CONFIG } from '@/shared/components/StatusBadge'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { Assinatura } from '@/features/assinaturas/types/assinatura'

const SCREEN_CODE = 'FIN-ASS-001'

const TIPO_LABELS: Record<string, string> = {
  STREAMING: 'Streaming',
  SOFTWARE: 'Software',
  ACADEMIA: 'Academia',
  OUTROS: 'Outros',
}

const columns: ColumnDef<Assinatura>[] = [
  { key: 'nome', label: 'Nome', sortable: true },
  {
    key: 'tipo',
    label: 'Tipo',
    sortable: true,
    render: (_v, row) => TIPO_LABELS[row.tipo] ?? row.tipo,
  },
  {
    key: 'valorMensal',
    label: 'Valor mensal',
    render: (_v, row) => formatBRL(row.valorMensal.valor),
  },
  {
    key: 'dataRenovacao',
    label: 'Renovacao',
    sortable: true,
    render: (_v, row) => formatDate(row.dataRenovacao),
  },
  {
    key: 'ativa',
    label: 'Status',
    render: (_v, row) => (
      <StatusBadge status={String(row.ativa)} config={CONTA_ATIVA_CONFIG} />
    ),
  },
]

export default function AssinaturasPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionada, setSelecionada] = useState<Assinatura | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const { data: itens, isLoading, isError } = useQuery({
    queryKey: ['assinaturas'],
    queryFn: assinaturaService.listar,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => assinaturaService.remover(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['assinaturas'] })
      setConfirmDeleteId(null)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Assinaturas</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="assinatura"
            entityId={selecionada?.id ?? null}
            screenCode={SCREEN_CODE}
          />
          <Button onClick={() => router.push('/assinaturas/nova')}>+ Nova</Button>
        </div>
      </div>

      {isError && <p className="text-sm text-destructive">Erro ao carregar assinaturas.</p>}

      {!isError && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={itens ?? []}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma assinatura cadastrada."
              onRowClick={setSelecionada}
              rowActions={(row) =>
                confirmDeleteId === row.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => deleteMutation.mutate(row.id)}
                      disabled={deleteMutation.isPending}
                    >
                      Confirmar
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => setConfirmDeleteId(null)}>
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Editar"
                      onClick={() => router.push(`/assinaturas/${row.id}/editar`)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Excluir"
                      onClick={() => setConfirmDeleteId(row.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                )
              }
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
