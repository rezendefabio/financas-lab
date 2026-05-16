import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'

// recharts ResponsiveContainer nao renderiza em jsdom sem dimensoes;
// substitui por um container com tamanho fixo para o grafico montar.
vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 600, height: 300 }}>{children}</div>
    ),
  }
})

import { GastosPorCategoriaChart } from './GastosPorCategoriaChart'
import type { GastosPorCategoria } from '../types/relatorio'

const comDados: GastosPorCategoria = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalGeral: { valor: 500, moeda: 'BRL' },
  itensPorCategoria: [
    { categoriaId: 'cat-1', nomeCategoria: 'Alimentacao', totalGasto: { valor: 300, moeda: 'BRL' } },
    { categoriaId: null, nomeCategoria: 'Sem categoria', totalGasto: { valor: 200, moeda: 'BRL' } },
  ],
}

const vazio: GastosPorCategoria = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalGeral: { valor: 0, moeda: 'BRL' },
  itensPorCategoria: [],
}

describe('GastosPorCategoriaChart', () => {
  it('renderiza o titulo do card', () => {
    render(<GastosPorCategoriaChart data={comDados} />)
    expect(screen.getByText('Gastos por categoria')).toBeInTheDocument()
  })

  it('exibe o total geral formatado em BRL quando ha dados', () => {
    render(<GastosPorCategoriaChart data={comDados} />)
    expect(screen.getByText(/Total geral/)).toBeInTheDocument()
    expect(screen.getByText(/500,00/)).toBeInTheDocument()
  })

  it('exibe mensagem de estado vazio quando nao ha itens', () => {
    render(<GastosPorCategoriaChart data={vazio} />)
    expect(screen.getByText('Nenhum dado no periodo')).toBeInTheDocument()
  })

  it('nao exibe o total geral quando o periodo esta vazio', () => {
    render(<GastosPorCategoriaChart data={vazio} />)
    expect(screen.queryByText(/Total geral/)).not.toBeInTheDocument()
  })
})
