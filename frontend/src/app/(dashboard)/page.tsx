'use client'
import { useQuery } from '@tanstack/react-query'
import { TrendingDown, TrendingUp } from 'lucide-react'
import { contasService } from '@/features/contas/services/contas.service'
import {
  getEvolucaoUltimosSeisMeses,
  getFluxoCaixa,
  getGastosMesAtual,
} from '@/features/dashboard'
import { OrcamentosProgressoCard } from '@/features/dashboard/components/OrcamentosProgressoCard'
import { EvolucaoSaldoChart, GastosPorCategoriaChart } from '@/features/relatorios'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { formatBRL } from '@/shared/lib/formatters'

export default function DashboardPage() {
  const { data: saldo, isLoading: isLoadingSaldo, isError: isErrorSaldo } = useQuery({
    queryKey: ['saldo-total'],
    queryFn: () => contasService.saldoTotal(),
  })

  const agora = new Date()
  const anoAtual = agora.getFullYear()
  const mesAtual = agora.getMonth() + 1

  const { data: fluxoCaixa, isLoading: isLoadingFluxo } = useQuery({
    queryKey: ['fluxo-caixa', anoAtual, mesAtual],
    queryFn: () => getFluxoCaixa(anoAtual, mesAtual),
  })

  const { data: gastos } = useQuery({
    queryKey: ['gastos-mes-atual'],
    queryFn: () => getGastosMesAtual(),
  })

  const { data: evolucao } = useQuery({
    queryKey: ['evolucao-6-meses'],
    queryFn: () => getEvolucaoUltimosSeisMeses(),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Card className="border-l-4 border-l-primary">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" />
              Saldo Total
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoadingSaldo && <div className="h-10 animate-pulse rounded bg-muted" />}
            {isErrorSaldo && <p className="text-sm text-destructive">Erro ao carregar saldo</p>}
            {saldo && (
              <>
                <p className="text-3xl font-bold tabular-nums text-foreground">
                  {formatBRL(saldo.valor)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {saldo.totalContas} {saldo.totalContas === 1 ? 'conta ativa' : 'contas ativas'}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="border-l-4 border-l-emerald-500">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-emerald-600" />
              Receitas do Mes
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoadingFluxo && <div className="h-10 animate-pulse rounded bg-muted" />}
            {fluxoCaixa && (
              <p className="text-3xl font-bold tabular-nums text-emerald-600">
                {formatBRL(fluxoCaixa.totalReceitas)}
              </p>
            )}
          </CardContent>
        </Card>

        <Card className="border-l-4 border-l-red-500">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
              <TrendingDown className="h-4 w-4 text-red-600" />
              Despesas do Mes
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoadingFluxo && <div className="h-10 animate-pulse rounded bg-muted" />}
            {fluxoCaixa && (
              <p className="text-3xl font-bold tabular-nums text-red-600">
                {formatBRL(fluxoCaixa.totalDespesas)}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {gastos ? (
          <GastosPorCategoriaChart data={gastos} />
        ) : (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-semibold">Gastos por categoria</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-72 animate-pulse rounded bg-muted" />
            </CardContent>
          </Card>
        )}
        <OrcamentosProgressoCard />
      </div>

      <div>
        {evolucao ? (
          <EvolucaoSaldoChart data={evolucao} />
        ) : (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-semibold">Evolucao do saldo</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-72 animate-pulse rounded bg-muted" />
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
