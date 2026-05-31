'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { StatusBadge, type StatusConfig } from '@/shared/components/StatusBadge'
import { Button } from '@/shared/components/ui/button'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import { exportToCsv } from '@/shared/lib/export-csv'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse, TipoEmprestimo } from '@/features/emprestimo'

const SCREEN_CODE = 'FIN-EMP-001'

const TIPO_LABELS: Record<TipoEmprestimo, string> = {
  CONCEDIDO: 'Concedido',
  RECEBIDO: 'Recebido',
}

const QUITADO_CONFIG: Record<string, StatusConfig> = {
  true: { label: 'Quitado', variant: 'default' },
  false: { label: 'Em aberto', variant: 'secondary' },
}

export default function EmprestimosPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [selecionada, setSelecionada] = useState<EmprestimoResponse | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['emprestimos'],
    queryFn: emprestimoService.listar,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => emprestimoService.deletar(id),
    onSuccess: async () => {
      setConfirmDeleteId(null)
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
    },
  })

  const emprestimos = data ?? []

  const columns: ColumnDef<EmprestimoResponse>[] = [
    { key: 'descricao', label: 'Descricao' },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (_v, row) => TIPO_LABELS[row.tipo],
    },
    {
      key: 'valor',
      label: 'Valor',
      className: 'text-right',
      render: (_v, row) => formatBRL(row.valor.valor),
    },
    {
      key: 'dataEmprestimo',
      label: 'Data',
      render: (_v, row) => formatDate(row.dataEmprestimo),
    },
    {
      key: 'quitado',
      label: 'Status',
      render: (_v, row) => (
        <StatusBadge status={String(row.quitado)} config={QUITADO_CONFIG} />
      ),
    },
  ]

  const handleExportCsv = () => {
    exportToCsv(
      'emprestimos',
      emprestimos.map((e) => ({
        descricao: e.descricao,
        nomeTerceiro: e.nomeTerceiro ?? '',
        tipo: TIPO_LABELS[e.tipo],
        valor: e.valor.valor,
        dataEmprestimo: e.dataEmprestimo,
        quitado: e.quitado ? 'Quitado' : 'Em aberto',
      })),
      [
        { key: 'descricao', label: 'Descricao' },
        { key: 'nomeTerceiro', label: 'Nome do terceiro' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'valor', label: 'Valor' },
        { key: 'dataEmprestimo', label: 'Data' },
        { key: 'quitado', label: 'Status' },
      ],
    )
  }

  const rowActions = (row: EmprestimoResponse) => {
    if (confirmDeleteId === row.id) {
      return (
        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="destructive"
            size="sm"
            disabled={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate(row.id)}
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
      <div className="flex justify-end gap-2">
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
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Emprestimos</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="emprestimo"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.descricao}
            screenCode={SCREEN_CODE}
            onExportCsv={handleExportCsv}
          />
          <Button type="button" onClick={() => router.push('/emprestimos/novo')}>
            Novo Emprestimo
          </Button>
        </div>
      </div>

      <DataTable
        data={emprestimos}
        columns={columns}
        keyField="id"
        isLoading={isLoading}
        emptyMessage="Nenhum emprestimo cadastrado."
        onRowClick={(row) => setSelecionada(row)}
        rowActions={rowActions}
      />
    </div>
  )
}
