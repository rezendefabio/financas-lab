import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import {
  listarCarteiras,
  buscarCarteira,
  criarCarteira,
  atualizarCarteira,
  deletarCarteira,
} from './carteira-service'

const mockCarteira = {
  id: '00000000-0000-0000-0000-000000000001',
  contaId: '00000000-0000-0000-0000-000000000002',
  nome: 'Tesouro',
  tipo: 'RENDA_FIXA' as const,
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarCarteiras', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockCarteira])

    const result = await listarCarteiras()

    expect(apiFetch).toHaveBeenCalledWith('/api/carteiras')
    expect(result).toEqual([mockCarteira])
  })
})

describe('buscarCarteira', () => {
  it('chama apiFetch com id no path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCarteira)

    const result = await buscarCarteira(mockCarteira.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/carteiras/${mockCarteira.id}`)
    expect(result).toEqual(mockCarteira)
  })
})

describe('criarCarteira', () => {
  it('chama apiFetch com POST e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCarteira)

    const payload = {
      contaId: mockCarteira.contaId,
      nome: 'Tesouro',
      tipo: 'RENDA_FIXA' as const,
    }
    const result = await criarCarteira(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/carteiras', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockCarteira)
  })
})

describe('atualizarCarteira', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCarteira)

    const payload = { nome: 'Nova', tipo: 'CRIPTOMOEDA' as const }
    const result = await atualizarCarteira(mockCarteira.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/carteiras/${mockCarteira.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockCarteira)
  })
})

describe('deletarCarteira', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await deletarCarteira(mockCarteira.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/carteiras/${mockCarteira.id}`, {
      method: 'DELETE',
    })
  })
})
