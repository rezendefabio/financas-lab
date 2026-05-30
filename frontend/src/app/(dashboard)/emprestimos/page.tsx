'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import {
  useEmprestimos,
  useExcluirEmprestimo,
  type EmprestimoResponse,
} from '@/features/emprestimo'

const SCREEN_CODE = 'FIN-EMP-001'

export default function EmprestimosPage() {
  const router = useRouter()
  const { data, isLoading } = useEmprestimos()
  const excluir = useExcluirEmprestimo()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const columns: ColumnDef<EmprestimoResponse>[] = [
    { key: 'descricao', label: 'Descricao' },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (v) => (v === 'CONCEDIDO' ? 'Concedido' : 'Recebido'),
    },
    {
      key: 'valor',
      label: 'Valor',
      render: (_v, row) => formatBRL(row.valor.valor),
    },
    {
      key: 'dataEmprestimo',
      label: 'Data',
      render: (v) => formatDate(String(v)),
    },
    {
      key: 'quitado',
      label: 'Status',
      render: (v) => (v ? 'Quitado' : 'Em aberto'),
    },
  ]

  const rowActions = (row: EmprestimoResponse) => {
    if (confirmDeleteId === row.id) {
      return (
        <div className="flex gap-2 justify-end">
          <Button
            size="sm"
            variant="destructive"
            onClick={() => {
              excluir.mutate(row.id, {
                onSuccess: () => setConfirmDeleteId(null),
              })
            }}
            disabled={excluir.isPending}
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
        </div>
      )
    }
    return (
      <div className="flex gap-2 justify-end">
        <Button
          size="sm"
          variant="outline"
          onClick={() => router.push(`/emprestimos/${row.id}/editar`)}
        >
          Editar
        </Button>
        <Button
          size="sm"
          variant="outline"
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

      <DataTable
        data={data ?? []}
        columns={columns}
        keyField="id"
        isLoading={isLoading}
        emptyMessage="Nenhum emprestimo cadastrado."
        rowActions={rowActions}
      />
    </div>
  )
}
