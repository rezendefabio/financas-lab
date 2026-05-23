import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

vi.mock('@/features/relatorios/services/relatorio-service', () => ({
  relatorioService: {
    getGastosPorCategoria: vi.fn(),
    getEvolucaoSaldo: vi.fn(),
  },
}))

import { apiFetch } from '@/services/api-client'
import { relatorioService } from '@/features/relatorios/services/relatorio-service'
import {
  getFluxoCaixa,
  getGastosMesAtual,
  getEvolucaoUltimosSeisMeses,
} from './dashboard-service'

function pad2(n: number) {
  return n.toString().padStart(2, '0')
}

describe('getFluxoCaixa', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('chama apiFetch com o path correto incluindo ano e mes', async () => {
    const fluxoMock = {
      ano: 2026,
      mes: 5,
      totalReceitas: 1000,
      totalDespesas: 300,
      saldo: 700,
      moeda: 'BRL',
    }
    vi.mocked(apiFetch).mockResolvedValue(fluxoMock)

    const result = await getFluxoCaixa(2026, 5)

    expect(apiFetch).toHaveBeenCalledWith('/api/relatorios/dashboard/fluxo-caixa?ano=2026&mes=5')
    expect(result).toEqual(fluxoMock)
  })

  it('propaga erro do apiFetch', async () => {
    vi.mocked(apiFetch).mockRejectedValue(new Error('Erro de API'))

    await expect(getFluxoCaixa(2026, 5)).rejects.toThrow('Erro de API')
  })

  it('chama apiFetch para mes diferente com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      ano: 2026,
      mes: 12,
      totalReceitas: 0,
      totalDespesas: 0,
      saldo: 0,
      moeda: 'BRL',
    })

    await getFluxoCaixa(2026, 12)

    expect(apiFetch).toHaveBeenCalledWith('/api/relatorios/dashboard/fluxo-caixa?ano=2026&mes=12')
  })
})

describe('getGastosMesAtual', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 4, 15))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('chama relatorioService.getGastosPorCategoria com primeiro dia do mes ate hoje', async () => {
    vi.mocked(relatorioService.getGastosPorCategoria).mockResolvedValue({
      dataInicio: '2026-05-01',
      dataFim: '2026-05-15',
      totalGeral: { valor: 0, moeda: 'BRL' },
      itensPorCategoria: [],
    })

    await getGastosMesAtual()

    expect(relatorioService.getGastosPorCategoria).toHaveBeenCalledWith(
      '2026-05-01',
      '2026-05-15',
    )
  })

  it('propaga erro do relatorioService', async () => {
    vi.mocked(relatorioService.getGastosPorCategoria).mockRejectedValue(
      new Error('falha'),
    )
    await expect(getGastosMesAtual()).rejects.toThrow('falha')
  })
})

describe('getEvolucaoUltimosSeisMeses', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 6, 20))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('chama relatorioService.getEvolucaoSaldo com dataInicio 6 meses atras (dia 1) ate hoje', async () => {
    vi.mocked(relatorioService.getEvolucaoSaldo).mockResolvedValue({
      dataInicio: '2026-01-01',
      dataFim: '2026-07-20',
      totalReceitas: { valor: 0, moeda: 'BRL' },
      totalDespesas: { valor: 0, moeda: 'BRL' },
      saldoLiquido: { valor: 0, moeda: 'BRL' },
      evolucaoPorMes: [],
    })

    await getEvolucaoUltimosSeisMeses()

    // Setado: 2026-07-20. Janela: month - 6 = 2026-01-01 ate 2026-07-20
    const esperadoInicio = '2026-01-01'
    const esperadoFim = `2026-${pad2(7)}-${pad2(20)}`
    expect(relatorioService.getEvolucaoSaldo).toHaveBeenCalledWith(
      esperadoInicio,
      esperadoFim,
    )
  })

  it('lida com virada de ano (mes <= 6)', async () => {
    vi.setSystemTime(new Date(2026, 2, 10)) // marco/2026
    vi.mocked(relatorioService.getEvolucaoSaldo).mockResolvedValue({
      dataInicio: '',
      dataFim: '',
      totalReceitas: { valor: 0, moeda: 'BRL' },
      totalDespesas: { valor: 0, moeda: 'BRL' },
      saldoLiquido: { valor: 0, moeda: 'BRL' },
      evolucaoPorMes: [],
    })

    await getEvolucaoUltimosSeisMeses()

    // marco/2026 - 6 meses = setembro/2025
    expect(relatorioService.getEvolucaoSaldo).toHaveBeenCalledWith(
      '2025-09-01',
      '2026-03-10',
    )
  })
})
