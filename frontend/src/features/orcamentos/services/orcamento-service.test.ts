import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { orcamentoService } from './orcamento-service'

const mockOrcamento = {
  id: '00000000-0000-0000-0000-000000000001',
  categoriaId: '00000000-0000-0000-0000-000000000002',
  valorLimite: { valor: 500, moeda: 'BRL' },
  mesAno: '2026-05-01',
  ativo: true,
  criadoEm: '2026-05-01T00:00:00Z',
  atualizadoEm: '2026-05-01T00:00:00Z',
}

const mockProgresso = {
  orcamentoId: '00000000-0000-0000-0000-000000000001',
  categoriaId: '00000000-0000-0000-0000-000000000002',
  mesAno: '2026-05-01',
  valorLimite: { valor: 500, moeda: 'BRL' },
  totalGasto: { valor: 200, moeda: 'BRL' },
  percentualUtilizado: 40,
  status: 'ABAIXO' as const,
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('orcamentoService', () => {
  it('listar() chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockOrcamento])

    const result = await orcamentoService.listar()

    expect(apiFetch).toHaveBeenCalledWith('/api/orcamentos')
    expect(result).toEqual([mockOrcamento])
  })

  it('buscar() chama apiFetch com path correto para o id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockOrcamento)

    const result = await orcamentoService.buscar(mockOrcamento.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/orcamentos/${mockOrcamento.id}`)
    expect(result).toEqual(mockOrcamento)
  })

  it('criar() chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockOrcamento)

    const payload = {
      categoriaId: mockOrcamento.categoriaId,
      valorLimiteValor: 500,
      valorLimiteMoeda: 'BRL',
      mesAno: '2026-05-01',
    }

    const result = await orcamentoService.criar(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/orcamentos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockOrcamento)
  })

  it('desativar() chama apiFetch com DELETE e id correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await orcamentoService.desativar(mockOrcamento.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/orcamentos/${mockOrcamento.id}`, {
      method: 'DELETE',
    })
  })

  it('progresso() chama apiFetch com path de progresso correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockProgresso)

    const result = await orcamentoService.progresso(mockOrcamento.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/orcamentos/${mockOrcamento.id}/progresso`)
    expect(result).toEqual(mockProgresso)
  })
})
