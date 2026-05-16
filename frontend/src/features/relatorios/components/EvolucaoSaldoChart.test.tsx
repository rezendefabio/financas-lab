import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'

vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 600, height: 300 }}>{children}</div>
    ),
  }
})

import { EvolucaoSaldoChart } from './EvolucaoSaldoChart'
import type { EvolucaoSaldo } from '../types/relatorio'

const comDados: EvolucaoSaldo = {
  dataInicio: '2026-01-01',
  dataFim: '2026-05-16',
  totalReceitas: { valor: 5000, moeda: 'BRL' },
  totalDespesas: { valor: 3000, moeda: 'BRL' },
  saldoLiquido: { valor: 2000, moeda: 'BRL' },
  evolucaoPorMes: [
    {
      mes: '2026-04-01',
      totalReceitas: { valor: 2000, moeda: 'BRL' },
      totalDespesas: { valor: 1500, moeda: 'BRL' },
      saldoLiquido: { valor: 500, moeda: 'BRL' },
    },
    {
      mes: '2026-05-01',
      totalReceitas: { valor: 3000, moeda: 'BRL' },
      totalDespesas: { valor: 1500, moeda: 'BRL' },
      saldoLiquido: { valor: 1500, moeda: 'BRL' },
    },
  ],
}

const vazio: EvolucaoSaldo = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalReceitas: { valor: 0, moeda: 'BRL' },
  totalDespesas: { valor: 0, moeda: 'BRL' },
  saldoLiquido: { valor: 0, moeda: 'BRL' },
  evolucaoPorMes: [],
}

describe('EvolucaoSaldoChart', () => {
  it('renderiza o titulo do card', () => {
    render(<EvolucaoSaldoChart data={comDados} />)
    expect(screen.getByText('Evolucao do saldo')).toBeInTheDocument()
  })

  it('exibe os tres cards de totais formatados em BRL', () => {
    render(<EvolucaoSaldoChart data={comDados} />)
    expect(screen.getByText('Total de receitas')).toBeInTheDocument()
    expect(screen.getByText('Total de despesas')).toBeInTheDocument()
    expect(screen.getByText('Saldo liquido')).toBeInTheDocument()
    expect(screen.getByText(/5\.000,00/)).toBeInTheDocument()
    expect(screen.getByText(/3\.000,00/)).toBeInTheDocument()
  })

  it('exibe mensagem de estado vazio quando nao ha meses', () => {
    render(<EvolucaoSaldoChart data={vazio} />)
    expect(screen.getByText('Nenhum dado no periodo')).toBeInTheDocument()
  })

  it('exibe os cards de totais mesmo com periodo vazio', () => {
    render(<EvolucaoSaldoChart data={vazio} />)
    expect(screen.getByText('Total de receitas')).toBeInTheDocument()
  })
})
