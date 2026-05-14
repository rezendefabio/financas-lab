import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MoneyInput } from './MoneyInput'

describe('MoneyInput', () => {
  it('renderiza com prefixo R$', () => {
    render(<MoneyInput value={0} onChange={vi.fn()} />)
    const input = screen.getByRole('textbox')
    expect(input).toBeInTheDocument()
    expect(input).toHaveValue('R$ 0,00')
  })

  it('exibe valor formatado quando nao esta em foco', () => {
    render(<MoneyInput value={1234.56} onChange={vi.fn()} />)
    const input = screen.getByRole('textbox')
    // formatBRL uses non-breaking space between R$ and value
    const expected = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(1234.56)
    expect(input).toHaveValue(expected)
  })

  it('onChange e chamado com numero correto ao digitar', () => {
    const onChange = vi.fn()
    render(<MoneyInput value={0} onChange={onChange} />)
    const input = screen.getByRole('textbox')
    fireEvent.focus(input)
    fireEvent.change(input, { target: { value: '100' } })
    expect(onChange).toHaveBeenCalledWith(1)
  })

  it('renderiza desabilitado quando disabled=true', () => {
    render(<MoneyInput value={0} onChange={vi.fn()} disabled />)
    const input = screen.getByRole('textbox')
    expect(input).toBeDisabled()
  })
})
