import { describe, it, expect } from 'vitest'
import { formatBRL, formatTipoConta, formatTipoTransacao, formatDate, formatTipoCategoria, formatDateTime, formatTamanho } from './formatters'

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

  it('returns label for INVESTIMENTO', () => {
    expect(formatTipoConta('INVESTIMENTO')).toBe('Investimento')
  })

  it('returns label for OUTRO', () => {
    expect(formatTipoConta('OUTRO')).toBe('Outro')
  })

  it('returns the original string for unknown type (fallback)', () => {
    expect(formatTipoConta('DESCONHECIDO')).toBe('DESCONHECIDO')
  })
})

describe('formatTipoCategoria', () => {
  it('retorna label em portugues', () => {
    expect(formatTipoCategoria('RECEITA')).toBe('Receita')
    expect(formatTipoCategoria('DESPESA')).toBe('Despesa')
    expect(formatTipoCategoria('DESCONHECIDO')).toBe('DESCONHECIDO')
  })
})

describe('formatTipoTransacao', () => {
  it('formatTipoTransacao retorna label em portugues', () => {
    expect(formatTipoTransacao('RECEITA')).toBe('Receita')
    expect(formatTipoTransacao('DESPESA')).toBe('Despesa')
    expect(formatTipoTransacao('TRANSFERENCIA')).toBe('Transferencia')
    expect(formatTipoTransacao('DESCONHECIDO')).toBe('DESCONHECIDO')
  })
})

describe('formatDate', () => {
  it('formata LocalDate (date-only) no padrao pt-BR', () => {
    expect(formatDate('2026-05-13')).toBe('13/05/2026')
  })

  it('formata Instant (string com T e Z) sem duplicar o componente de hora', () => {
    const result = formatDate('2026-05-14T15:30:00Z')
    expect(result).not.toBe('Invalid Date')
    expect(result).toMatch(/\d{2}\/\d{2}\/\d{4}/)
  })

  it('retorna -- para null', () => {
    expect(formatDate(null)).toBe('--')
  })

  it('retorna -- para undefined', () => {
    expect(formatDate(undefined)).toBe('--')
  })
})

describe('formatDateTime', () => {
  it('formata Instant como data e hora no locale pt-BR', () => {
    const result = formatDateTime('2026-05-14T15:30:00Z')
    expect(result).not.toBe('Invalid Date')
    expect(result).toContain('2026')
  })

  it('retorna -- para null', () => {
    expect(formatDateTime(null)).toBe('--')
  })

  it('retorna -- para undefined', () => {
    expect(formatDateTime(undefined)).toBe('--')
  })
})

describe('formatTamanho', () => {
  it('formata bytes abaixo de 1KB com unidade B', () => {
    expect(formatTamanho(512)).toBe('512 B')
  })

  it('formata kilobytes com uma casa decimal', () => {
    expect(formatTamanho(2048)).toBe('2.0 KB')
  })

  it('formata megabytes com uma casa decimal', () => {
    expect(formatTamanho(5 * 1024 * 1024)).toBe('5.0 MB')
  })

  it('formata gigabytes com uma casa decimal', () => {
    expect(formatTamanho(3 * 1024 * 1024 * 1024)).toBe('3.0 GB')
  })
})
