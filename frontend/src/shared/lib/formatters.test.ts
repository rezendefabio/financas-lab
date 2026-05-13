import { describe, it, expect } from 'vitest'
import { formatBRL, formatTipoConta, formatTipoCategoria } from './formatters'

describe('formatBRL', () => {
  it('formats a number as BRL currency', () => {
    expect(formatBRL(1234.56)).toBe('R$ 1.234,56')
  })

  it('formats zero correctly', () => {
    expect(formatBRL(0)).toBe('R$ 0,00')
  })
})

describe('formatTipoConta', () => {
  it('returns label for CORRENTE', () => {
    expect(formatTipoConta('CORRENTE')).toBe('Conta Corrente')
  })

  it('returns label for POUPANCA', () => {
    expect(formatTipoConta('POUPANCA')).toBe('Poupanca')
  })

  it('returns label for DINHEIRO', () => {
    expect(formatTipoConta('DINHEIRO')).toBe('Dinheiro')
  })

  it('returns label for CARTAO_CREDITO', () => {
    expect(formatTipoConta('CARTAO_CREDITO')).toBe('Cartao de Credito')
  })

  it('returns the original string for unknown type (fallback)', () => {
    expect(formatTipoConta('DESCONHECIDO')).toBe('DESCONHECIDO')
  })
})

describe('formatTipoCategoria', () => {
  it('formatTipoCategoria retorna label em portugues', () => {
    expect(formatTipoCategoria('RECEITA')).toBe('Receita')
    expect(formatTipoCategoria('DESPESA')).toBe('Despesa')
    expect(formatTipoCategoria('DESCONHECIDO')).toBe('DESCONHECIDO')
  })
})
