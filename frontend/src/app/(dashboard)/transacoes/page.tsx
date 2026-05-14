'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatTipoTransacao, formatDate } from '@/shared/lib/formatters'
import type { Transacao } from '@/features/transacoes/types/transacao'

function badgeVariant(tipo: string) {
  if (tipo === 'RECEITA') return 'default' as const
  if (tipo === 'DESPESA') return 'destructive' as const
  return 'secondary' as const
}

function TransacaoCard({ transacao }: { transacao: Transacao }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base truncate max-w-[60%]">
            {transacao.descricao}
          </CardTitle>
          <Badge variant={badgeVariant(transacao.tipo)}>
            {formatTipoTransacao(transacao.tipo)}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-xl font-bold tabular-nums">
          {formatBRL(transacao.valor)}
        </p>
        <p className="text-xs text-muted-foreground mt-1">{formatDate(transacao.data)}</p>
      </CardContent>
    </Card>
  )
}

export default function TransacoesPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['transacoes'],
    queryFn: () => transacoesService.listar({ size: 20 }),
  })

  const transacoes = data?.content ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Transacoes</h1>
        <Button onClick={() => router.push('/transacoes/novo')}>Nova Transacao</Button>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-40" />
              </CardHeader>
              <CardContent className="space-y-2">
                <Skeleton className="h-6 w-28" />
                <Skeleton className="h-4 w-20" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar transacoes.</p>
      )}

      {!isLoading && !isError && transacoes.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma transacao cadastrada.</p>
          <Button onClick={() => router.push('/transacoes/novo')}>Registrar primeira transacao</Button>
        </div>
      )}

      {transacoes.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {transacoes.map((t) => (
            <TransacaoCard key={t.id} transacao={t} />
          ))}
        </div>
      )}

      {data && data.totalElements > 20 && (
        <p className="text-xs text-muted-foreground text-center">
          Exibindo 20 de {data.totalElements} transacoes.
        </p>
      )}
    </div>
  )
}
