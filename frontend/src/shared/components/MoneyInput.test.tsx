import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MoneyInput } from './MoneyInput'

describe('MoneyInput', () => {
  it('renderiza placeholder R$ 0,00 quando value=0', () => {
    render(<MoneyInput value={0} onChange={vi.fn()} />)
    const input = screen.getByRole('textbox')
    expect(input).toBeInTheDocument()
    expect(input).toHaveAttribute('placeholder', 'R$ 0,00')
  })

  it('formata valor 1500 como R$ 1.500,00', () => {
    render(<MoneyInput value={1500} onChange={vi.fn()} />)
    const input = screen.getByRole('textbox')
    expect(input).toHaveValue('R$ 1.500,00')
  })

  it('formata valor 1234.56 como R$ 1.234,56', () => {
    render(<MoneyInput value={1234.56} onChange={vi.fn()} />)
    const input = screen.getByRole('textbox')
    expect(input).toHaveValue('R$ 1.234,56')
  })

  it('renderiza desabilitado quando disabled=true', () => {
    render(<MoneyInput value={0} onChange={vi.fn()} disabled />)
    const input = screen.getByRole('textbox')
    expect(input).toBeDisabled()
  })

  it('chama onChange com floatValue correto apos digitacao', async () => {
    const onChange = vi.fn()
    render(<MoneyInput value={0} onChange={onChange} />)
    const input = screen.getByRole('textbox')
    await userEvent.type(input, '1500')
    expect(onChange).toHaveBeenCalled()
    const lastCall = onChange.mock.calls[onChange.mock.calls.length - 1]
    expect(typeof lastCall[0]).toBe('number')
  })

  it('nao aceita valor acima de 999.999.999,99', async () => {
    const onChange = vi.fn()
    render(<MoneyInput value={999_999_999.99} onChange={onChange} />)
    const input = screen.getByRole('textbox')
    expect(input).toHaveValue('R$ 999.999.999,99')
  })

  it('aplica id quando fornecido', () => {
    render(<MoneyInput value={0} onChange={vi.fn()} id="campo-valor" />)
    const input = screen.getByRole('textbox')
    expect(input).toHaveAttribute('id', 'campo-valor')
  })
})
