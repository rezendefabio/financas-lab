import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import {
  listarLimites,
  buscarLimite,
  criarLimite,
  atualizarLimite,
  desativarLimite,
} from './limite-service'

const mockLimite = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000002',
  nome: 'Limite Mensal',
  tipo: 'MENSAL' as const,
  valor: 500,
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarLimites', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockLimite])

    const result = await listarLimites()

    expect(apiFetch).toHaveBeenCalledWith('/api/limites')
    expect(result).toEqual([mockLimite])
  })
})

describe('buscarLimite', () => {
  it('chama apiFetch com id no path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLimite)

    const result = await buscarLimite(mockLimite.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/limites/${mockLimite.id}`)
    expect(result).toEqual(mockLimite)
  })
})

describe('criarLimite', () => {
  it('chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLimite)

    const payload = { nome: 'Limite Mensal', tipo: 'MENSAL' as const, valor: 500 }
    const result = await criarLimite(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/limites', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockLimite)
  })
})

describe('atualizarLimite', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLimite)

    const payload = { nome: 'Renomeado', tipo: 'ANUAL' as const, valor: 1000 }
    const result = await atualizarLimite(mockLimite.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/limites/${mockLimite.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockLimite)
  })
})

describe('desativarLimite', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await desativarLimite(mockLimite.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/limites/${mockLimite.id}`, {
      method: 'DELETE',
    })
  })
})
