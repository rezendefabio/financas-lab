import { cn } from '@/shared/lib/utils'
import { formatBRL } from '@/shared/lib/formatters'
import { StatCard } from '@/shared/components/StatCard'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import type { FluxoCaixa } from '../types/dashboard'

interface FluxoCaixaCardProps {
  data: FluxoCaixa
  isLoading?: boolean
}

const MESES = [
  'Janeiro', 'Fevereiro', 'Marco', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

function FluxoCaixaCard({ data, isLoading = false }: FluxoCaixaCardProps) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">Fluxo de Caixa</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">Carregando...</p>
        </CardContent>
      </Card>
    )
  }

  const nomeMes = MESES[data.mes - 1] ?? String(data.mes)
  const saldoPositivo = data.saldo >= 0

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold">
          {`Fluxo de Caixa -- ${nomeMes}/${data.ano}`}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard
            titulo="Receitas"
            valor={formatBRL(data.totalReceitas)}
            className="border-emerald-200 bg-emerald-50"
          />
          <StatCard
            titulo="Despesas"
            valor={formatBRL(data.totalDespesas)}
            className="border-red-200 bg-red-50"
          />
          <StatCard
            titulo="Saldo"
            valor={formatBRL(data.saldo)}
            className={cn(
              saldoPositivo
                ? 'border-emerald-200 bg-emerald-50'
                : 'border-red-200 bg-red-50',
            )}
          />
        </div>
        <p className="mt-3 text-xs text-muted-foreground">
          {`Moeda: ${data.moeda}`}
        </p>
      </CardContent>
    </Card>
  )
}

export { FluxoCaixaCard }
export default FluxoCaixaCard
