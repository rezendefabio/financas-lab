'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarTags, deletarTag } from '@/features/tag'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { exportToCsv } from '@/shared/lib/export-csv'
import type { Tag } from '@/features/tag/types/tag'

const SCREEN_CODE = 'FIN-TAG-001'

export default function TagsPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmandoId, setConfirmandoId] = useState<string | null>(null)
  const [selecionada, setSelecionada] = useState<Tag | null>(null)

  const { data: tags = [], isLoading, isError } = useQuery({
    queryKey: ['tags'],
    queryFn: listarTags,
  })

  const deletarMutation = useMutation({
    mutationFn: (id: string) => deletarTag(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['tags'] })
      setConfirmandoId(null)
    },
  })

  const columns: ColumnDef<Tag>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'cor',
      label: 'Cor',
      render: (value) =>
        value ? (
          <span className="flex items-center gap-2">
            <span
              className="inline-block h-4 w-4 rounded-full border border-border"
              style={{ backgroundColor: String(value) }}
              aria-label={`Cor: ${String(value)}`}
            />
            <span className="text-sm text-muted-foreground">
              {String(value)}
            </span>
          </span>
        ) : (
          <span className="text-sm text-muted-foreground">—</span>
        ),
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'tags',
      tags.map((t) => ({ nome: t.nome })),
      [{ key: 'nome', label: 'Nome' }],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Tags</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="tag"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.nome}
            screenCode={SCREEN_CODE}
            onExportCsv={tags.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/tags/novo')}>+ Nova Tag</Button>
        </div>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar tags.</p>
      )}

      {!isError && !isLoading && tags.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma tag cadastrada.</p>
          <Button onClick={() => router.push('/tags/novo')}>
            Criar primeira tag
          </Button>
        </div>
      )}

      {!isError && (isLoading || tags.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={tags}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma tag encontrada."
              rowActions={(tag) =>
                confirmandoId === tag.id ? (
                  <span className="flex items-center justify-end gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      disabled={deletarMutation.isPending}
                      onClick={() => deletarMutation.mutate(tag.id)}
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
                      variant="ghost"
                      aria-label={`Historico de ${tag.nome}`}
                      onClick={() => setSelecionada(tag)}
                    >
                      Log
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => router.push(`/tags/${tag.id}/editar`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setConfirmandoId(tag.id)}
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
