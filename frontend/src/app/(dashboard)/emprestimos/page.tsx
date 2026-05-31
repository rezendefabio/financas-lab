'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import { exportToCsv } from '@/shared/lib/export-csv'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse } from '@/features/emprestimo'

const SCREEN_CODE = 'FIN-EMP-001'

const TIPO_LABEL: Record<string, string> = {
  CONCEDIDO: 'Concedido',
  RECEBIDO: 'Recebido',
}

export default function EmprestimosPage() {
  const router = useRouter()
  const queryClient = useQueryClient()

  const [filtro, setFiltro] = React.useState('')
  const [selecionada, setSelecionada] = React.useState<EmprestimoResponse | null>(
    null,
  )
  const [confirmDeleteId, setConfirmDeleteId] = React.useState<string | null>(
    null,
  )

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

  const emprestimos = React.useMemo(() => {
    const termo = filtro.trim().toLowerCase()
    const lista = data ?? []
    if (!termo) return lista
    return lista.filter(
      (e) =>
        e.descricao.toLowerCase().includes(termo) ||
        (e.nomeTerceiro ?? '').toLowerCase().includes(termo),
    )
  }, [data, filtro])

  const columns: ColumnDef<EmprestimoResponse>[] = [
    { key: 'descricao', label: 'Descricao' },
    {
      key: 'nomeTerceiro',
      label: 'Terceiro',
      render: (v) => (v as string | null) ?? '-',
    },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (v) => TIPO_LABEL[v as string] ?? String(v),
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
      render: (v) => formatDate(v as string),
    },
    {
      key: 'quitado',
      label: 'Quitado',
      render: (v) => ((v as boolean) ? 'Sim' : 'Nao'),
    },
  ]

  const rowActions = (row: EmprestimoResponse) =>
    confirmDeleteId === row.id ? (
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
    ) : (
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

  return (
    <div className="space-y-4" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">Emprestimos</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="emprestimo"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.descricao}
            screenCode={SCREEN_CODE}
            onExportCsv={() =>
              exportToCsv(
                emprestimos.map((e) => ({
                  descricao: e.descricao,
                  nomeTerceiro: e.nomeTerceiro ?? '',
                  tipo: TIPO_LABEL[e.tipo] ?? e.tipo,
                  valor: e.valor.valor,
                  dataEmprestimo: e.dataEmprestimo,
                  quitado: e.quitado ? 'Sim' : 'Nao',
                })),
                [
                  { key: 'descricao', label: 'Descricao' },
                  { key: 'nomeTerceiro', label: 'Terceiro' },
                  { key: 'tipo', label: 'Tipo' },
                  { key: 'valor', label: 'Valor' },
                  { key: 'dataEmprestimo', label: 'Data' },
                  { key: 'quitado', label: 'Quitado' },
                ],
                'emprestimos',
              )
            }
          />
          <Button type="button" onClick={() => router.push('/emprestimos/nova')}>
            Novo Emprestimo
          </Button>
        </div>
      </div>

      <Input
        placeholder="Filtrar por descricao ou terceiro..."
        value={filtro}
        onChange={(e) => setFiltro(e.target.value)}
        className="max-w-sm"
      />

      <DataTable
        data={emprestimos}
        columns={columns}
        keyField="id"
        isLoading={isLoading}
        emptyMessage="Nenhum emprestimo encontrado."
        onRowClick={(row) => setSelecionada(row)}
        rowActions={rowActions}
      />
    </div>
  )
}
