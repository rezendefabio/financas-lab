import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { FluxoCaixaCard } from './FluxoCaixaCard'
import type { FluxoCaixa } from '../types/dashboard'

const fluxoBase: FluxoCaixa = {
  ano: 2026,
  mes: 5,
  totalReceitas: 1000,
  totalDespesas: 300,
  saldo: 700,
  moeda: 'BRL',
}

describe('FluxoCaixaCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza titulo com mes e ano', () => {
    render(<FluxoCaixaCard data={fluxoBase} />)
    expect(screen.getByText(/Fluxo de Caixa/)).toBeInTheDocument()
    expect(screen.getByText(/Maio\/2026/)).toBeInTheDocument()
  })

  it('exibe totalReceitas formatado em BRL', () => {
    render(<FluxoCaixaCard data={fluxoBase} />)
    // Usa regex para tolerar variacao de locale no separador de milhar
    expect(screen.getByText(/1[.,]000/)).toBeInTheDocument()
  })

  it('exibe totalDespesas formatado em BRL', () => {
    render(<FluxoCaixaCard data={fluxoBase} />)
    expect(screen.getByText(/300,00/)).toBeInTheDocument()
  })

  it('exibe saldo formatado em BRL', () => {
    render(<FluxoCaixaCard data={fluxoBase} />)
    expect(screen.getByText(/700,00/)).toBeInTheDocument()
  })

  it('exibe moeda BRL', () => {
    render(<FluxoCaixaCard data={fluxoBase} />)
    expect(screen.getByText(/BRL/)).toBeInTheDocument()
  })

  it('exibe estado de loading quando isLoading=true', () => {
    render(<FluxoCaixaCard data={fluxoBase} isLoading />)
    expect(screen.getByText('Carregando...')).toBeInTheDocument()
  })

  it('nao exibe dados quando isLoading=true', () => {
    render(<FluxoCaixaCard data={fluxoBase} isLoading />)
    expect(screen.queryByText(/Maio\/2026/)).not.toBeInTheDocument()
  })

  it('renderiza corretamente para mes de dezembro', () => {
    const fluxoDezembro: FluxoCaixa = { ...fluxoBase, mes: 12, ano: 2025 }
    render(<FluxoCaixaCard data={fluxoDezembro} />)
    expect(screen.getByText(/Dezembro\/2025/)).toBeInTheDocument()
  })

  it('renderiza saldo negativo quando despesas superam receitas', () => {
    const fluxoNegativo: FluxoCaixa = {
      ...fluxoBase,
      totalReceitas: 100,
      totalDespesas: 500,
      saldo: -400,
    }
    render(<FluxoCaixaCard data={fluxoNegativo} />)
    expect(screen.getByText(/400/)).toBeInTheDocument()
  })
})
