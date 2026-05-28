'use client'
import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarFaturas, deletarFatura } from '@/features/fatura'
import type { FaturaResponse } from '@/features/fatura'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { formatBRL, formatDate } from '@/shared/lib/formatters'

const SCREEN_CODE = 'FIN-FAT-001'

export default function FaturasPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmExcluirId, setConfirmExcluirId] = useState<string | null>(null)

  const { data: faturas = [], isLoading, isError } = useQuery({
    queryKey: ['faturas'],
    queryFn: listarFaturas,
  })

  const { data: contas = [] } = useQuery({
    queryKey: ['contas', 'lookup'],
    queryFn: () => contasService.listar(),
  })

  const contaNomePorId = useMemo(() => {
    const map = new Map<string, string>()
    for (const c of contas) {
      map.set(c.id, c.nome)
    }
    return map
  }, [contas])

  const excluirMutation = useMutation({
    mutationFn: (id: string) => deletarFatura(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['faturas'] })
      setConfirmExcluirId(null)
    },
  })

  const columns: ColumnDef<FaturaResponse>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'contaId',
      label: 'Conta',
      render: (value) => contaNomePorId.get(String(value)) ?? '—',
    },
    {
      key: 'dataVencimento',
      label: 'Vencimento',
      sortable: true,
      render: (value) => formatDate(value as string),
    },
    {
      key: 'valorTotal',
      label: 'Valor Total',
      className: 'text-right tabular-nums',
      render: (_value, row) =>
        row.valorTotal ? formatBRL(row.valorTotal.valor) : '—',
    },
    {
      key: 'paga',
      label: 'Paga',
      render: (value) =>
        value ? (
          <Badge className="bg-green-600 hover:bg-green-600">Paga</Badge>
        ) : (
          <Badge className="bg-yellow-500 hover:bg-yellow-500 text-black">
            Pendente
          </Badge>
        ),
    },
  ]

  return (
    <div className="space-y-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Faturas</h1>
        <Button onClick={() => router.push('/faturas/nova')}>+ Nova fatura</Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar faturas.</p>
      )}

      {!isError && !isLoading && faturas.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma fatura cadastrada.</p>
          <Button onClick={() => router.push('/faturas/nova')}>
            Criar primeira fatura
          </Button>
        </div>
      )}

      {!isError && (isLoading || faturas.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={faturas}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma fatura encontrada."
              rowActions={(fatura) =>
                confirmExcluirId === fatura.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => excluirMutation.mutate(fatura.id)}
                      disabled={excluirMutation.isPending}
                    >
                      Confirmar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmExcluirId(null)}
                    >
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => router.push(`/faturas/${fatura.id}`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => setConfirmExcluirId(fatura.id)}
                    >
                      Excluir
                    </Button>
                  </span>
                )
              }
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
