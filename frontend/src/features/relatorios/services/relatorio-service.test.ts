import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { relatorioService } from './relatorio-service'
import type { GastosPorCategoria, EvolucaoSaldo } from '../types/relatorio'

const gastosMock: GastosPorCategoria = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalGeral: { valor: 500, moeda: 'BRL' },
  itensPorCategoria: [
    { categoriaId: 'cat-1', nomeCategoria: 'Alimentacao', totalGasto: { valor: 300, moeda: 'BRL' } },
    { categoriaId: null, nomeCategoria: 'Sem categoria', totalGasto: { valor: 200, moeda: 'BRL' } },
  ],
}

const evolucaoMock: EvolucaoSaldo = {
  dataInicio: '2026-01-01',
  dataFim: '2026-05-16',
  totalReceitas: { valor: 5000, moeda: 'BRL' },
  totalDespesas: { valor: 3000, moeda: 'BRL' },
  saldoLiquido: { valor: 2000, moeda: 'BRL' },
  evolucaoPorMes: [
    {
      mes: '2026-05-01',
      totalReceitas: { valor: 1000, moeda: 'BRL' },
      totalDespesas: { valor: 600, moeda: 'BRL' },
      saldoLiquido: { valor: 400, moeda: 'BRL' },
    },
  ],
}

describe('relatorioService', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getGastosPorCategoria', () => {
    it('monta a URL com dataInicio e dataFim e omite contaId quando undefined', async () => {
      vi.mocked(apiFetch).mockResolvedValue(gastosMock)

      const result = await relatorioService.getGastosPorCategoria('2026-05-01', '2026-05-16')

      expect(apiFetch).toHaveBeenCalledWith(
        '/api/relatorios/gastos-por-categoria?dataInicio=2026-05-01&dataFim=2026-05-16',
      )
      expect(result).toEqual(gastosMock)
    })

    it('inclui contaId na query string quando informado', async () => {
      vi.mocked(apiFetch).mockResolvedValue(gastosMock)

      await relatorioService.getGastosPorCategoria('2026-05-01', '2026-05-16', 'conta-99')

      expect(apiFetch).toHaveBeenCalledWith(
        '/api/relatorios/gastos-por-categoria?dataInicio=2026-05-01&dataFim=2026-05-16&contaId=conta-99',
      )
    })

    it('propaga erro do apiFetch', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new Error('Erro de API'))

      await expect(
        relatorioService.getGastosPorCategoria('2026-05-01', '2026-05-16'),
      ).rejects.toThrow('Erro de API')
    })
  })

  describe('getEvolucaoSaldo', () => {
    it('monta a URL com dataInicio e dataFim e omite contaId quando undefined', async () => {
      vi.mocked(apiFetch).mockResolvedValue(evolucaoMock)

      const result = await relatorioService.getEvolucaoSaldo('2026-01-01', '2026-05-16')

      expect(apiFetch).toHaveBeenCalledWith(
        '/api/relatorios/evolucao-saldo?dataInicio=2026-01-01&dataFim=2026-05-16',
      )
      expect(result).toEqual(evolucaoMock)
    })

    it('inclui contaId na query string quando informado', async () => {
      vi.mocked(apiFetch).mockResolvedValue(evolucaoMock)

      await relatorioService.getEvolucaoSaldo('2026-01-01', '2026-05-16', 'conta-7')

      expect(apiFetch).toHaveBeenCalledWith(
        '/api/relatorios/evolucao-saldo?dataInicio=2026-01-01&dataFim=2026-05-16&contaId=conta-7',
      )
    })

    it('propaga erro do apiFetch', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new Error('Falha de rede'))

      await expect(
        relatorioService.getEvolucaoSaldo('2026-01-01', '2026-05-16'),
      ).rejects.toThrow('Falha de rede')
    })
  })
})
