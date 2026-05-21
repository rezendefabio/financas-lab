import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import {
  listarCentrosCusto,
  buscarCentroCusto,
  criarCentroCusto,
  atualizarCentroCusto,
  desativarCentroCusto,
} from './centrocusto-service'

const mockCentroCusto = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000002',
  nome: 'Casa',
  descricao: 'Despesas residenciais',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarCentrosCusto', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockCentroCusto])

    const result = await listarCentrosCusto()

    expect(apiFetch).toHaveBeenCalledWith('/api/centros-custo')
    expect(result).toEqual([mockCentroCusto])
  })
})

describe('buscarCentroCusto', () => {
  it('chama apiFetch com id no path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCentroCusto)

    const result = await buscarCentroCusto(mockCentroCusto.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/centros-custo/${mockCentroCusto.id}`)
    expect(result).toEqual(mockCentroCusto)
  })
})

describe('criarCentroCusto', () => {
  it('chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCentroCusto)

    const payload = { nome: 'Casa', descricao: 'Despesas residenciais' }
    const result = await criarCentroCusto(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/centros-custo', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockCentroCusto)
  })

  it('aceita payload sem descricao', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCentroCusto)

    await criarCentroCusto({ nome: 'Trabalho' })

    expect(apiFetch).toHaveBeenCalledWith('/api/centros-custo', {
      method: 'POST',
      body: JSON.stringify({ nome: 'Trabalho' }),
    })
  })
})

describe('atualizarCentroCusto', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockCentroCusto)

    const payload = { nome: 'Casa Renomeada' }
    const result = await atualizarCentroCusto(mockCentroCusto.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/centros-custo/${mockCentroCusto.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockCentroCusto)
  })
})

describe('desativarCentroCusto', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await desativarCentroCusto(mockCentroCusto.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/centros-custo/${mockCentroCusto.id}`, {
      method: 'DELETE',
    })
  })
})
