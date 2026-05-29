'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarLimites, desativarLimite } from '@/features/limite'
import { TIPO_LIMITE_LABEL } from '@/features/limite/types/tipo-limite'
import type { Limite } from '@/features/limite/types/limite'
import { formatBRL } from '@/shared/lib/formatters'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'

const SCREEN_CODE = 'CAD-LMT-001'

export default function LimitesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmDesativarId, setConfirmDesativarId] = useState<string | null>(null)

  const { data: limites = [], isLoading, isError } = useQuery({
    queryKey: ['limites'],
    queryFn: listarLimites,
  })

  const desativarMutation = useMutation({
    mutationFn: (id: string) => desativarLimite(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['limites'] })
      setConfirmDesativarId(null)
    },
  })

  const columns: ColumnDef<Limite>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (value) => TIPO_LIMITE_LABEL[value as Limite['tipo']] ?? String(value),
    },
    {
      key: 'valor',
      label: 'Valor',
      className: 'text-right',
      render: (value) => formatBRL(Number(value)),
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
        <h1 className="text-2xl font-semibold tracking-tight">Limites</h1>
        <Button onClick={() => router.push('/limites/novo')}>+ Novo Limite</Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar limites.</p>
      )}

      {!isError && !isLoading && limites.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhum limite cadastrado.</p>
          <Button onClick={() => router.push('/limites/novo')}>
            Criar primeiro limite
          </Button>
        </div>
      )}

      {!isError && (isLoading || limites.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={limites}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum limite encontrado."
              rowActions={(limite) =>
                confirmDesativarId === limite.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => desativarMutation.mutate(limite.id)}
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
                      onClick={() => router.push(`/limites/${limite.id}`)}
                    >
                      Editar
                    </Button>
                    {limite.ativo && (
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => setConfirmDesativarId(limite.id)}
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
