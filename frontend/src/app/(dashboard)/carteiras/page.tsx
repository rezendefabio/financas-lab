'use client'
import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarCarteiras, deletarCarteira } from '@/features/carteira'
import type { CarteiraResponse, TipoCarteira } from '@/features/carteira'
import {
  TIPO_CARTEIRA_LABEL,
  TIPO_CARTEIRA_BADGE_CLASS,
} from '@/features/carteira/types/tipo-carteira'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'

const SCREEN_CODE = 'FIN-CTR-001'

export default function CarteirasPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmExcluirId, setConfirmExcluirId] = useState<string | null>(null)

  const { data: carteiras = [], isLoading, isError } = useQuery({
    queryKey: ['carteiras'],
    queryFn: listarCarteiras,
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
    mutationFn: (id: string) => deletarCarteira(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['carteiras'] })
      setConfirmExcluirId(null)
    },
  })

  const columns: ColumnDef<CarteiraResponse>[] = [
    { key: 'nome', label: 'Nome', sortable: true, className: 'font-medium' },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (value) => {
        const tipo = value as TipoCarteira
        return (
          <Badge className={TIPO_CARTEIRA_BADGE_CLASS[tipo]}>
            {TIPO_CARTEIRA_LABEL[tipo]}
          </Badge>
        )
      },
    },
    {
      key: 'contaId',
      label: 'Conta',
      render: (value) => contaNomePorId.get(String(value)) ?? '—',
    },
    {
      key: 'ativo',
      label: 'Ativo',
      render: (value) =>
        value ? (
          <Badge className="bg-green-600 hover:bg-green-600">Ativo</Badge>
        ) : (
          <Badge className="bg-gray-500 hover:bg-gray-500">Inativo</Badge>
        ),
    },
  ]

  return (
    <div className="space-y-6" data-screen-code={SCREEN_CODE}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Carteiras</h1>
        <Button onClick={() => router.push('/carteiras/nova')}>+ Nova carteira</Button>
      </div>

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar carteiras.</p>
      )}

      {!isError && !isLoading && carteiras.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma carteira cadastrada.</p>
          <Button onClick={() => router.push('/carteiras/nova')}>
            Criar primeira carteira
          </Button>
        </div>
      )}

      {!isError && (isLoading || carteiras.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={carteiras}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma carteira encontrada."
              rowActions={(carteira) =>
                confirmExcluirId === carteira.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => excluirMutation.mutate(carteira.id)}
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
                      onClick={() => router.push(`/carteiras/${carteira.id}`)}
                    >
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => setConfirmExcluirId(carteira.id)}
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
