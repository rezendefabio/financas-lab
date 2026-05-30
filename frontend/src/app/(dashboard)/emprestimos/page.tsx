'use client'

import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { StatusBadge, type StatusConfig } from '@/shared/components/StatusBadge'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import {
  useEmprestimos,
  useDeletarEmprestimo,
  type EmprestimoResponse,
} from '@/features/emprestimo'

const SCREEN_CODE = 'FIN-EMP-001'

const TIPO_LABELS: Record<string, string> = {
  CONCEDIDO: 'Concedido',
  RECEBIDO: 'Recebido',
}

const QUITADO_CONFIG: Record<string, StatusConfig> = {
  true: { label: 'Quitado', variant: 'secondary' },
  false: { label: 'Em aberto', variant: 'default' },
}

export default function EmprestimosPage() {
  const router = useRouter()
  const { data, isLoading } = useEmprestimos()
  const deletarMutation = useDeletarEmprestimo()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const columns = useMemo<ColumnDef<EmprestimoResponse>[]>(
    () => [
      {
        key: 'descricao',
        label: 'Descricao',
        render: (_, row) => row.descricao,
      },
      {
        key: 'tipo',
        label: 'Tipo',
        render: (_, row) => TIPO_LABELS[row.tipo] ?? row.tipo,
      },
      {
        key: 'valor',
        label: 'Valor',
        render: (_, row) => formatBRL(row.valor.valor),
      },
      {
        key: 'dataEmprestimo',
        label: 'Data',
        render: (_, row) => formatDate(row.dataEmprestimo),
      },
      {
        key: 'quitado',
        label: 'Situacao',
        render: (_, row) => (
          <StatusBadge
            status={String(row.quitado)}
            config={QUITADO_CONFIG}
          />
        ),
      },
    ],
    [],
  )

  const rowActions = (row: EmprestimoResponse) => {
    if (confirmDeleteId === row.id) {
      return (
        <div className="flex gap-2 justify-end">
          <Button
            type="button"
            variant="destructive"
            size="sm"
            onClick={() => {
              deletarMutation.mutate(row.id, {
                onSettled: () => setConfirmDeleteId(null),
              })
            }}
            disabled={deletarMutation.isPending}
          >
            Confirmar
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setConfirmDeleteId(null)}
          >
            Cancelar
          </Button>
        </div>
      )
    }
    return (
      <div className="flex gap-2 justify-end">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => router.push(`/emprestimos/${row.id}/editar`)}
        >
          Editar
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setConfirmDeleteId(row.id)}
        >
          Excluir
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-4" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Emprestimos</h1>
        <Button onClick={() => router.push('/emprestimos/novo')}>
          Novo emprestimo
        </Button>
      </div>

      <DataTable<EmprestimoResponse>
        data={data ?? []}
        columns={columns}
        keyField="id"
        isLoading={isLoading}
        rowActions={rowActions}
        emptyMessage="Nenhum emprestimo cadastrado."
      />
    </div>
  )
}
