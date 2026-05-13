'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatTipoConta } from '@/shared/lib/formatters'
import type { Conta } from '@/features/contas/types/conta'

function ContaCard({ conta, onClick }: { conta: Conta; onClick: () => void }) {
  return (
    <Card
      className="cursor-pointer hover:bg-muted/50 transition-colors"
      onClick={onClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">{conta.nome}</CardTitle>
          <Badge variant={conta.ativa ? 'default' : 'secondary'}>
            {conta.ativa ? 'Ativa' : 'Inativa'}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">{formatTipoConta(conta.tipo)}</p>
        <p className="text-lg font-semibold mt-1 tabular-nums">
          <span className="tabular-nums font-medium">{formatBRL(conta.saldoInicialValor)}</span>
        </p>
        <p className="text-xs text-muted-foreground">saldo inicial</p>
      </CardContent>
    </Card>
  )
}

export default function ContasPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['contas'],
    queryFn: contasService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Contas</h1>
        <Button onClick={() => router.push('/contas/novo')}>Nova Conta</Button>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-32" />
              </CardHeader>
              <CardContent className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-6 w-28" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar contas.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma conta cadastrada.</p>
          <Button onClick={() => router.push('/contas/novo')}>Criar primeira conta</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((conta) => (
            <ContaCard
              key={conta.id}
              conta={conta}
              onClick={() => router.push(`/contas/${conta.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
