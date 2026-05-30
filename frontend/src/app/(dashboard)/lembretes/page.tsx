'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  useLembretes,
  useExcluirLembrete,
  PRIORIDADE_LEMBRETE_LABEL,
} from '@/features/lembrete'
import type { Lembrete } from '@/features/lembrete'
import { formatDate } from '@/shared/lib/formatters'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'

const SCREEN_CODE = 'MOD-LMB-001'

export default function LembretesPage() {
  const router = useRouter()
  const [confirmExcluirId, setConfirmExcluirId] = useState<string | null>(null)

  const { data: lembretes = [], isLoading, isError } = useLembretes()

  const excluirMutation = useExcluirLembrete()

  const columns: ColumnDef<Lembrete>[] = [
    { key: 'titulo', label: 'Titulo', sortable: true, className: 'font-medium' },
    {
      key: 'dataLembrete',
      label: 'Data',
      render: (value) => formatDate(String(value)),
    },
    {
      key: 'prioridade',
      label: 'Prioridade',
      render: (value) =>
        PRIORIDADE_LEMBRETE_LABEL[value as Lembrete['prioridade']] ?? String(value),
    },
    {
      key: 'concluido',
      label: 'Status',
      render: (value) => (value ? 'Sim' : 'Nao'),
    },
  ]

  return (
    <div className="space-y-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Lembretes</h1>
        <Button onClick={() => router.push('/lembretes/novo')}>+ Novo Lembrete</Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar lembretes.</p>
      )}

      {!isError && !isLoading && lembretes.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhum lembrete cadastrado.</p>
          <Button onClick={() => router.push('/lembretes/novo')}>
            Criar primeiro lembrete
          </Button>
        </div>
      )}

      {!isError && (isLoading || lembretes.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={lembretes}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum lembrete encontrado."
              rowActions={(lembrete) =>
                confirmExcluirId === lembrete.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() =>
                        excluirMutation.mutate(lembrete.id, {
                          onSuccess: () => setConfirmExcluirId(null),
                        })
                      }
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
                      onClick={() => router.push(`/lembretes/${lembrete.id}`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => setConfirmExcluirId(lembrete.id)}
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
