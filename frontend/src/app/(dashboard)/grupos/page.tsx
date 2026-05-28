'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarGrupos, deletarGrupo } from '@/features/grupo'
import type { Grupo } from '@/features/grupo'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'

const SCREEN_CODE = 'CAD-GRP-001'

export default function GruposPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmandoId, setConfirmandoId] = useState<string | null>(null)

  const { data: grupos = [], isLoading, isError } = useQuery({
    queryKey: ['grupos'],
    queryFn: listarGrupos,
  })

  const deletarMutation = useMutation({
    mutationFn: (id: string) => deletarGrupo(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['grupos'] })
      setConfirmandoId(null)
    },
  })

  const columns: ColumnDef<Grupo>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'descricao',
      label: 'Descricao',
      render: (value) => (value ? String(value) : '—'),
    },
    {
      key: 'ativo',
      label: 'Ativo',
      render: (value) => (value ? 'Sim' : 'Nao'),
    },
  ]

  return (
    <div className="space-y-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Grupos</h1>
        <Button onClick={() => router.push('/grupos/nova')}>+ Novo grupo</Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar grupos.</p>
      )}

      {!isError && !isLoading && grupos.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhum grupo cadastrado.</p>
          <Button onClick={() => router.push('/grupos/nova')}>
            Criar primeiro grupo
          </Button>
        </div>
      )}

      {!isError && (isLoading || grupos.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={grupos}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum grupo encontrado."
              rowActions={(grupo) =>
                confirmandoId === grupo.id ? (
                  <span className="flex items-center justify-end gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      disabled={deletarMutation.isPending}
                      onClick={() => deletarMutation.mutate(grupo.id)}
                    >
                      Confirmar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmandoId(null)}
                    >
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <span className="flex items-center justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => router.push(`/grupos/${grupo.id}`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setConfirmandoId(grupo.id)}
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
