'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { StatusBadge, type StatusConfig } from '@/shared/components/StatusBadge'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { Emprestimo } from '@/features/emprestimo/types/emprestimo'

const SCREEN_CODE = 'FIN-EMP-001'

const TIPO_CONFIG: Record<string, StatusConfig> = {
  CONCEDIDO: { label: 'Concedido', variant: 'default' },
  RECEBIDO: { label: 'Recebido', variant: 'secondary' },
}

const QUITADO_CONFIG: Record<string, StatusConfig> = {
  true: { label: 'Quitado', variant: 'default' },
  false: { label: 'Em aberto', variant: 'outline' },
}

const columns: ColumnDef<Emprestimo>[] = [
  { key: 'descricao', label: 'Descricao', sortable: true },
  {
    key: 'tipo',
    label: 'Tipo',
    sortable: true,
    render: (_value, row) => (
      <StatusBadge status={row.tipo} config={TIPO_CONFIG} />
    ),
  },
  {
    key: 'valor',
    label: 'Valor',
    sortable: false,
    render: (_value, row) => formatBRL(row.valor.valor),
  },
  {
    key: 'dataEmprestimo',
    label: 'Data',
    sortable: true,
    render: (_value, row) => formatDate(row.dataEmprestimo),
  },
  {
    key: 'quitado',
    label: 'Situacao',
    sortable: true,
    render: (_value, row) => (
      <StatusBadge status={String(row.quitado)} config={QUITADO_CONFIG} />
    ),
  },
]

export default function EmprestimosPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionado, setSelecionado] = useState<Emprestimo | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const { data: itens, isLoading, isError } = useQuery({
    queryKey: ['emprestimos'],
    queryFn: emprestimoService.listar,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => emprestimoService.remover(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      setConfirmDeleteId(null)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Emprestimos</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="emprestimo"
            entityId={selecionado?.id ?? null}
            screenCode={SCREEN_CODE}
          />
          <Button onClick={() => router.push('/emprestimos/novo')}>
            + Novo
          </Button>
        </div>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar emprestimos.</p>
      )}

      {!isError && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={itens ?? []}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum emprestimo cadastrado."
              onRowClick={setSelecionado}
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
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmDeleteId(null)}
                    >
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Editar"
                      onClick={() =>
                        router.push(`/emprestimos/${row.id}/editar`)
                      }
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
