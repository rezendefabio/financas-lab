'use client'
import { useQuery } from '@tanstack/react-query'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { formatBRL } from '@/shared/lib/formatters'

export default function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['saldo-total'],
    queryFn: () => contasService.saldoTotal(),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Saldo Total
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading && (
              <div className="h-8 animate-pulse rounded bg-muted" />
            )}
            {isError && (
              <p className="text-sm text-destructive">Erro ao carregar saldo</p>
            )}
            {data && (
              <>
                <p className="text-2xl font-bold tabular-nums">
                  <span className="tabular-nums font-medium">{formatBRL(data.valor)}</span>
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {data.totalContas} {data.totalContas === 1 ? 'conta ativa' : 'contas ativas'}
                </p>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
