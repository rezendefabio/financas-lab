'use client'
import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { listarPayees, deletarPayee } from '@/features/payee/services/payee-service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { exportToCsv } from '@/shared/lib/export-csv'
import type { Payee } from '@/features/payee/types/payee'

const SCREEN_CODE = 'FIN-PAY-001'

export default function PayeesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [selecionado, setSelecionado] = useState<Payee | null>(null)

  const { data: payees = [], isLoading, isError } = useQuery({
    queryKey: ['payees'],
    queryFn: listarPayees,
  })

  const { data: categorias = [] } = useQuery({
    queryKey: ['categorias'],
    queryFn: () => categoriasService.listar(),
  })

  const categoriaPorId = useMemo(
    () => new Map(categorias.map((c) => [c.id, c.nome])),
    [categorias],
  )

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletarPayee(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payees'] })
      setConfirmDeleteId(null)
    },
  })

  const columns: ColumnDef<Payee>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'categoriaPadraoId',
      label: 'Categoria Padrao',
      render: (value) =>
        value ? (categoriaPorId.get(String(value)) ?? '—') : '—',
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'payees',
      payees.map((p) => ({
        nome: p.nome,
        categoriaPadrao: p.categoriaPadraoId
          ? (categoriaPorId.get(p.categoriaPadraoId) ?? '')
          : '',
      })),
      [
        { key: 'nome', label: 'Nome' },
        { key: 'categoriaPadrao', label: 'Categoria Padrao' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Beneficiarios</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="payee"
            entityId={selecionado?.id ?? null}
            entityLabel={selecionado?.nome}
            screenCode={SCREEN_CODE}
            onExportCsv={payees.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/payees/novo')}>
            + Novo Beneficiario
          </Button>
        </div>
      </div>

      {isError && (
        <p className="text-sm text-destructive">
          Erro ao carregar beneficiarios.
        </p>
      )}

      {!isError && !isLoading && payees.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">
            Nenhum beneficiario cadastrado.
          </p>
          <Button onClick={() => router.push('/payees/novo')}>
            Criar primeiro beneficiario
          </Button>
        </div>
      )}

      {!isError && (isLoading || payees.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={payees}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum beneficiario encontrado."
              rowActions={(payee) =>
                confirmDeleteId === payee.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => deleteMutation.mutate(payee.id)}
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
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Historico de ${payee.nome}`}
                      onClick={() => setSelecionado(payee)}
                    >
                      Log
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => router.push(`/payees/${payee.id}/editar`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => setConfirmDeleteId(payee.id)}
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
