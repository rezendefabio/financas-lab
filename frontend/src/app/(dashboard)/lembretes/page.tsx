'use client'

import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { StatusBadge, type StatusConfig } from '@/shared/components/StatusBadge'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { formatDate } from '@/shared/lib/formatters'
import {
  lembreteService,
  useLembretes,
  type LembreteResponse,
} from '@/features/lembrete'

const SCREEN_CODE = 'MOD-LEM-001'

const PRIORIDADE_CONFIG: Record<string, StatusConfig> = {
  BAIXA: { label: 'Baixa', variant: 'outline' },
  MEDIA: { label: 'Media', variant: 'secondary' },
  ALTA: { label: 'Alta', variant: 'default' },
}

const CONCLUIDO_CONFIG: Record<string, StatusConfig> = {
  true: { label: 'Concluido', variant: 'secondary' },
  false: { label: 'Pendente', variant: 'outline' },
}

export default function LembretesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { data, isLoading } = useLembretes()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [selecionado, setSelecionado] = useState<LembreteResponse | null>(null)

  const deleteMutation = useMutation({
    mutationFn: (id: string) => lembreteService.deletar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lembretes'] })
      setConfirmDeleteId(null)
    },
  })

  const lembretes = useMemo(() => data ?? [], [data])

  const columns: ColumnDef<LembreteResponse>[] = [
    { key: 'titulo', label: 'Titulo' },
    {
      key: 'dataLembrete',
      label: 'Data',
      render: (v) => formatDate(v as string),
    },
    {
      key: 'prioridade',
      label: 'Prioridade',
      render: (v) => (
        <StatusBadge status={String(v)} config={PRIORIDADE_CONFIG} />
      ),
    },
    {
      key: 'concluido',
      label: 'Status',
      render: (v) => (
        <StatusBadge status={String(v)} config={CONCLUIDO_CONFIG} />
      ),
    },
  ]

  return (
    <div className="space-y-4 p-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Lembretes</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="lembrete"
            entityId={selecionado?.id ?? null}
            entityLabel={selecionado?.titulo}
            screenCode={SCREEN_CODE}
          />
          <Button onClick={() => router.push('/lembretes/novo')}>
            Novo lembrete
          </Button>
        </div>
      </div>

      <DataTable<LembreteResponse>
        data={lembretes}
        columns={columns}
        keyField="id"
        isLoading={isLoading}
        emptyMessage="Nenhum lembrete encontrado."
        onRowClick={(row) => setSelecionado(row)}
        rowActions={(row) => (
          <div className="flex items-center justify-end gap-2">
            {confirmDeleteId === row.id ? (
              <>
                <Button
                  size="sm"
                  variant="destructive"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate(row.id)}
                >
                  Confirmar
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setConfirmDeleteId(null)}
                >
                  Cancelar
                </Button>
              </>
            ) : (
              <>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => router.push(`/lembretes/${row.id}/editar`)}
                >
                  Editar
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setConfirmDeleteId(row.id)}
                >
                  Excluir
                </Button>
              </>
            )}
          </div>
        )}
      />
    </div>
  )
}
