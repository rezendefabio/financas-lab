'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { metaService } from '@/features/metas/services/meta-service'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { Meta, StatusMeta } from '@/features/metas/types/meta'

function statusVariant(status: StatusMeta): 'default' | 'secondary' | 'outline' | 'destructive' {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'default'
    case 'CONCLUIDA':
      return 'secondary'
    case 'CANCELADA':
      return 'outline'
    default:
      return 'default'
  }
}

function statusLabel(status: StatusMeta): string {
  switch (status) {
    case 'EM_ANDAMENTO':
      return 'Em Andamento'
    case 'CONCLUIDA':
      return 'Concluida'
    case 'CANCELADA':
      return 'Cancelada'
    default:
      return status
  }
}

function MetaRow({ meta, onVer }: { meta: Meta; onVer: () => void }) {
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30 transition-colors">
      <td className="py-3 px-4 text-sm">{meta.nome}</td>
      <td className="py-3 px-4 text-sm">{formatDate(meta.prazo)}</td>
      <td className="py-3 px-4 text-sm text-right tabular-nums">{formatBRL(meta.valorAlvo.valor)}</td>
      <td className="py-3 px-4 text-sm">
        <div className="flex items-center gap-2">
          <Badge variant={statusVariant(meta.status)}>{statusLabel(meta.status)}</Badge>
          {meta.atrasada && meta.status === 'EM_ANDAMENTO' && (
            <Badge variant="destructive">Atrasada</Badge>
          )}
        </div>
      </td>
      <td className="py-3 px-4 text-sm">
        <Button variant="ghost" size="sm" onClick={onVer}>
          Ver
        </Button>
      </td>
    </tr>
  )
}

export default function MetasPage() {
  const router = useRouter()

  const { data: metas, isLoading, isError } = useQuery({
    queryKey: ['metas'],
    queryFn: metaService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Metas</h1>
        <Button onClick={() => router.push('/metas/novo')}>+ Nova Meta</Button>
      </div>

      {isLoading && (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar metas.</p>
      )}

      {metas && metas.length === 0 && (
        <p className="text-muted-foreground">Nenhuma meta cadastrada.</p>
      )}

      {metas && metas.length > 0 && (
        <div className="rounded-md border">
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Nome</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Prazo</th>
                <th className="py-3 px-4 text-right text-sm font-medium text-muted-foreground">Valor Alvo</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Status</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Acoes</th>
              </tr>
            </thead>
            <tbody>
              {metas.map((meta) => (
                <MetaRow
                  key={meta.id}
                  meta={meta}
                  onVer={() => router.push(`/metas/${meta.id}`)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
