'use client'
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
} from 'recharts'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { StatCard } from '@/shared/components/StatCard'
import type { EvolucaoSaldo } from '../types/relatorio'

interface EvolucaoSaldoChartProps {
  data: EvolucaoSaldo
}

// recharts tipa os formatters com valores possivelmente undefined / nao-numericos.
function formatarEixoBRL(valor: unknown): string {
  return typeof valor === 'number' ? formatBRL(valor) : ''
}

function rotuloMes(mesIso: string): string {
  // mesIso vem como data ISO (LocalDate, ex: 2026-05-01); exibir MM/YYYY
  const data = formatDate(mesIso)
  const partes = data.split('/')
  return partes.length === 3 ? `${partes[1]}/${partes[2]}` : data
}

function EvolucaoSaldoChart({ data }: EvolucaoSaldoChartProps) {
  const meses = data.evolucaoPorMes ?? []
  const vazio = meses.length === 0

  const dadosGrafico = meses.map((item) => ({
    mes: rotuloMes(item.mes),
    Receitas: item.totalReceitas.valor,
    Despesas: item.totalDespesas.valor,
    Saldo: item.saldoLiquido.valor,
  }))

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold">Evolucao do saldo</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard
            titulo="Total de receitas"
            valor={formatBRL(data.totalReceitas.valor)}
            className="border-emerald-200 bg-emerald-50"
          />
          <StatCard
            titulo="Total de despesas"
            valor={formatBRL(data.totalDespesas.valor)}
            className="border-red-200 bg-red-50"
          />
          <StatCard
            titulo="Saldo liquido"
            valor={formatBRL(data.saldoLiquido.valor)}
            className={
              data.saldoLiquido.valor >= 0
                ? 'border-emerald-200 bg-emerald-50'
                : 'border-red-200 bg-red-50'
            }
          />
        </div>
        {vazio ? (
          <p className="text-sm text-muted-foreground">Nenhum dado no periodo</p>
        ) : (
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 1, height: 1 }}>
              <BarChart
                data={dadosGrafico}
                margin={{ top: 8, right: 16, bottom: 8, left: 8 }}
              >
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="mes" fontSize={11} />
                <YAxis tickFormatter={formatarEixoBRL} fontSize={11} />
                <Tooltip
                  formatter={formatarEixoBRL}
                  labelStyle={{ color: '#0f172a' }}
                />
                <Legend />
                <Bar dataKey="Receitas" fill="#16a34a" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Despesas" fill="#dc2626" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Saldo" fill="#2563eb" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export { EvolucaoSaldoChart }
export default EvolucaoSaldoChart
