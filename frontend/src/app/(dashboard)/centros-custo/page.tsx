'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import {
  listarCentrosCusto,
  desativarCentroCusto,
} from '@/features/centrocusto/services/centrocusto-service'
import type { CentroCusto } from '@/features/centrocusto/types/centrocusto'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'

const SCREEN_CODE = 'CAD-CCU-001'

export default function CentrosCustoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmDesativarId, setConfirmDesativarId] = useState<string | null>(null)

  const { data: centros = [], isLoading, isError } = useQuery({
    queryKey: ['centros-custo'],
    queryFn: listarCentrosCusto,
  })

  const desativarMutation = useMutation({
    mutationFn: (id: string) => desativarCentroCusto(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['centros-custo'] })
      setConfirmDesativarId(null)
    },
  })

  const columns: ColumnDef<CentroCusto>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'descricao',
      label: 'Descricao',
      render: (value) => (value ? String(value) : '—'),
    },
    {
      key: 'ativo',
      label: 'Situacao',
      render: (value) => (value ? 'Ativo' : 'Inativo'),
    },
  ]

  return (
    <div className="space-y-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Centros de Custo</h1>
        <Button onClick={() => router.push('/centros-custo/novo')}>
          + Novo Centro de Custo
        </Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">
          Erro ao carregar centros de custo.
        </p>
      )}

      {!isError && !isLoading && centros.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">
            Nenhum centro de custo cadastrado.
          </p>
          <Button onClick={() => router.push('/centros-custo/novo')}>
            Criar primeiro centro de custo
          </Button>
        </div>
      )}

      {!isError && (isLoading || centros.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={centros}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum centro de custo encontrado."
              rowActions={(centro) =>
                confirmDesativarId === centro.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => desativarMutation.mutate(centro.id)}
                      disabled={desativarMutation.isPending}
                    >
                      Confirmar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmDesativarId(null)}
                    >
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => router.push(`/centros-custo/${centro.id}`)}
                    >
                      Editar
                    </Button>
                    {centro.ativo && (
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => setConfirmDesativarId(centro.id)}
                      >
                        Desativar
                      </Button>
                    )}
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
