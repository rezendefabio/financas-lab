import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { FluxoCaixaResumo } from './FluxoCaixaResumo'
import type { FluxoCaixa } from '@/features/dashboard'

const fluxoBase: FluxoCaixa = {
  ano: 2026,
  mes: 5,
  totalReceitas: 4000,
  totalDespesas: 1500,
  saldo: 2500,
  moeda: 'BRL',
}

describe('FluxoCaixaResumo', () => {
  it('renderiza titulo com mes e ano', () => {
    render(<FluxoCaixaResumo data={fluxoBase} />)
    expect(screen.getByText(/Fluxo de Caixa/)).toBeInTheDocument()
    expect(screen.getByText(/Maio\/2026/)).toBeInTheDocument()
  })

  it('exibe receitas, despesas e saldo formatados em BRL', () => {
    render(<FluxoCaixaResumo data={fluxoBase} />)
    expect(screen.getByText(/4\.000,00/)).toBeInTheDocument()
    expect(screen.getByText(/1\.500,00/)).toBeInTheDocument()
    expect(screen.getByText(/2\.500,00/)).toBeInTheDocument()
  })

  it('exibe estado de loading quando isLoading=true', () => {
    render(<FluxoCaixaResumo data={fluxoBase} isLoading />)
    expect(screen.getByText('Carregando...')).toBeInTheDocument()
  })
})
