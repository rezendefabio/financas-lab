import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { listarPayees, criarPayee, atualizarPayee, deletarPayee } from './payee-service'

const mockPayee = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000002',
  nome: 'Supermercado',
  categoriaPadraoId: undefined,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarPayees', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockPayee])

    const result = await listarPayees()

    expect(apiFetch).toHaveBeenCalledWith('/api/payees')
    expect(result).toEqual([mockPayee])
  })
})

describe('criarPayee', () => {
  it('chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockPayee)

    const payload = { nome: 'Supermercado' }
    const result = await criarPayee(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/payees', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockPayee)
  })

  it('chama apiFetch com categoriaPadraoId quando fornecido', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockPayee)

    const payload = { nome: 'Farmacia', categoriaPadraoId: 'cat-uuid' }
    await criarPayee(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/payees', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('atualizarPayee', () => {
  it('chama apiFetch com PUT e id e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockPayee)

    const id = mockPayee.id
    const payload = { nome: 'Mercado Extra' }
    const result = await atualizarPayee(id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/payees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockPayee)
  })
})

describe('deletarPayee', () => {
  it('chama apiFetch com DELETE e id correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    const id = mockPayee.id
    await deletarPayee(id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/payees/${id}`, {
      method: 'DELETE',
    })
  })
})
