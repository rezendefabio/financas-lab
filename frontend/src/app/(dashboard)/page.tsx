'use client'
import { useQuery } from '@tanstack/react-query'
import { TrendingUp } from 'lucide-react'
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
        <Card className="sm:col-span-2 lg:col-span-1 border-l-4 border-l-primary">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" />
              Saldo Total
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading && <div className="h-10 animate-pulse rounded bg-muted" />}
            {isError && <p className="text-sm text-destructive">Erro ao carregar saldo</p>}
            {data && (
              <>
                <p className="text-3xl font-bold tabular-nums text-foreground">
                  {formatBRL(data.valor)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {data.totalContas} {data.totalContas === 1 ? 'conta ativa' : 'contas ativas'}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="border-dashed">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Transacoes este mes
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-muted-foreground/40">—</p>
            <p className="text-xs text-muted-foreground mt-1">em breve</p>
          </CardContent>
        </Card>

        <Card className="border-dashed">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Orcamento do mes
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-muted-foreground/40">—</p>
            <p className="text-xs text-muted-foreground mt-1">em breve</p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
