import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import {
  listarFaturas,
  buscarFatura,
  criarFatura,
  atualizarFatura,
  deletarFatura,
} from './fatura-service'

const mockFatura = {
  id: '00000000-0000-0000-0000-000000000001',
  contaId: '00000000-0000-0000-0000-000000000002',
  nome: 'Cartao Maio',
  dataVencimento: '2026-06-10',
  dataFechamento: '2026-06-03',
  valorTotal: { valor: 1500, moeda: 'BRL' },
  paga: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarFaturas', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockFatura])

    const result = await listarFaturas()

    expect(apiFetch).toHaveBeenCalledWith('/api/faturas')
    expect(result).toEqual([mockFatura])
  })
})

describe('buscarFatura', () => {
  it('chama apiFetch com id no path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockFatura)

    const result = await buscarFatura(mockFatura.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/faturas/${mockFatura.id}`)
    expect(result).toEqual(mockFatura)
  })
})

describe('criarFatura', () => {
  it('chama apiFetch com POST e payload completo', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockFatura)

    const payload = {
      contaId: mockFatura.contaId,
      nome: 'Cartao Maio',
      dataVencimento: '2026-06-10',
      dataFechamento: '2026-06-03',
      valorTotalValor: 1500,
      valorTotalMoeda: 'BRL',
    }
    const result = await criarFatura(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/faturas', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockFatura)
  })

  it('aceita payload minimo sem valor total', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockFatura)

    const payload = {
      contaId: mockFatura.contaId,
      nome: 'Cartao',
      dataVencimento: '2026-06-10',
    }
    await criarFatura(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/faturas', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('atualizarFatura', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockFatura)

    const payload = { nome: 'Cartao Junho', dataVencimento: '2026-07-10' }
    const result = await atualizarFatura(mockFatura.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/faturas/${mockFatura.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockFatura)
  })
})

describe('deletarFatura', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await deletarFatura(mockFatura.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/faturas/${mockFatura.id}`, {
      method: 'DELETE',
    })
  })
})
