'use client'

import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'
import type { Emprestimo } from '@/features/emprestimo/types/emprestimo'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { Badge } from '@/shared/components/ui/badge'
import { formatBRL, formatDate } from '@/shared/lib/formatters'

const SCREEN_CODE = 'FIN-EMP-001'

const TIPO_LABEL: Record<string, string> = {
  CONCEDIDO: 'Concedido',
  RECEBIDO: 'Recebido',
}

const columns: ColumnDef<Emprestimo>[] = [
  { key: 'descricao', label: 'Descricao' },
  {
    key: 'tipo',
    label: 'Tipo',
    render: (v) => <Badge variant="outline">{TIPO_LABEL[String(v)] ?? String(v)}</Badge>,
  },
  {
    key: 'valor',
    label: 'Valor',
    render: (_, row) => formatBRL(row.valor.valor),
  },
  {
    key: 'dataEmprestimo',
    label: 'Data',
    render: (v) => formatDate(String(v)),
  },
  {
    key: 'quitado',
    label: 'Status',
    render: (v) =>
      v ? (
        <Badge variant="default">Quitado</Badge>
      ) : (
        <Badge variant="secondary">Em aberto</Badge>
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

  const ordenados = useMemo(() => {
    if (!itens) return []
    return [...itens].sort((a, b) =>
      a.dataEmprestimo < b.dataEmprestimo ? 1 : a.dataEmprestimo > b.dataEmprestimo ? -1 : 0,
    )
  }, [itens])

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
          <Button onClick={() => router.push('/emprestimos/novo')}>+ Novo</Button>
        </div>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar emprestimos.</p>
      )}

      {!isError && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={ordenados}
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
                      onClick={() => router.push(`/emprestimos/${row.id}/editar`)}
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
