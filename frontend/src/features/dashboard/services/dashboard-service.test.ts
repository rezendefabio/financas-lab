import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { getFluxoCaixa } from './dashboard-service'

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
