import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatCard } from './StatCard'

describe('StatCard', () => {
  it('renderiza titulo e valor', () => {
    render(<StatCard titulo="Total" valor="R$ 1.234,56" />)
    expect(screen.getByText('Total')).toBeInTheDocument()
    expect(screen.getByText('R$ 1.234,56')).toBeInTheDocument()
  })

  it('renderiza variacao positiva com cor verde', () => {
    render(
      <StatCard titulo="Saldo" valor="R$ 500,00" variacao={{ valor: '+12%', positiva: true }} />
    )
    const variacao = screen.getByText('+12%')
    expect(variacao).toHaveClass('text-emerald-600')
  })

  it('renderiza variacao negativa com cor vermelha', () => {
    render(
      <StatCard titulo="Saldo" valor="R$ 500,00" variacao={{ valor: '-R$ 50,00', positiva: false }} />
    )
    const variacao = screen.getByText('-R$ 50,00')
    expect(variacao).toHaveClass('text-destructive')
  })

  it('nao renderiza variacao quando nao passada', () => {
    const { container } = render(<StatCard titulo="Total" valor="R$ 0,00" />)
    // No variation element should be present
    expect(container.querySelectorAll('.text-emerald-600')).toHaveLength(0)
    expect(container.querySelectorAll('.text-destructive')).toHaveLength(0)
  })

  it('renderiza descricao quando passada', () => {
    render(<StatCard titulo="Total" valor="R$ 0,00" descricao="Ultimo mes" />)
    expect(screen.getByText('Ultimo mes')).toBeInTheDocument()
  })

  it('nao renderiza descricao quando nao passada', () => {
    render(<StatCard titulo="Total" valor="R$ 0,00" />)
    expect(screen.queryByText('Ultimo mes')).not.toBeInTheDocument()
  })
})
