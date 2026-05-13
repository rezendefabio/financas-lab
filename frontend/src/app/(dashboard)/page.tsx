'use client'
import { useQuery } from '@tanstack/react-query'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'

function formatBRL(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
}

export default function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['saldo-total'],
    queryFn: () => contasService.saldoTotal(),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
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
                <p className="text-2xl font-bold">{formatBRL(data.valor)}</p>
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
