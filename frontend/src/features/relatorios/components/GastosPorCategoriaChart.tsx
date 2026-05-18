'use client'
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
} from 'recharts'
import { formatBRL } from '@/shared/lib/formatters'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import type { GastosPorCategoria } from '../types/relatorio'

interface GastosPorCategoriaChartProps {
  data: GastosPorCategoria
}

const CORES = [
  '#2563eb', '#16a34a', '#dc2626', '#d97706', '#7c3aed',
  '#0891b2', '#db2777', '#65a30d', '#ea580c', '#4f46e5',
]

// recharts tipa os formatters com valores possivelmente undefined / nao-numericos.
function formatarEixoBRL(valor: unknown): string {
  return typeof valor === 'number' ? formatBRL(valor) : ''
}

function GastosPorCategoriaChart({ data }: GastosPorCategoriaChartProps) {
  const itens = data.itensPorCategoria ?? []
  const vazio = itens.length === 0

  const dadosGrafico = itens.map((item) => ({
    nome: item.nomeCategoria,
    valor: item.totalGasto.valor,
  }))

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold">Gastos por categoria</CardTitle>
      </CardHeader>
      <CardContent>
        {vazio ? (
          <p className="text-sm text-muted-foreground">Nenhum dado no periodo</p>
        ) : (
          <>
            <p className="mb-3 text-sm text-muted-foreground">
              Total geral:{' '}
              <span className="font-semibold tabular-nums text-foreground">
                {formatBRL(data.totalGeral.valor)}
              </span>
            </p>
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 1, height: 1 }}>
                <BarChart
                  layout="vertical"
                  data={dadosGrafico}
                  margin={{ top: 8, right: 16, bottom: 8, left: 8 }}
                >
                  <XAxis
                    type="number"
                    tickFormatter={formatarEixoBRL}
                    fontSize={11}
                  />
                  <YAxis
                    type="category"
                    dataKey="nome"
                    width={120}
                    fontSize={11}
                  />
                  <Tooltip
                    formatter={formatarEixoBRL}
                    labelStyle={{ color: '#0f172a' }}
                  />
                  <Bar dataKey="valor" radius={[0, 4, 4, 0]}>
                    {dadosGrafico.map((_, index) => (
                      <Cell key={index} fill={CORES[index % CORES.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

export { GastosPorCategoriaChart }
export default GastosPorCategoriaChart
