import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import {
  listarLembretes,
  buscarLembrete,
  criarLembrete,
  atualizarLembrete,
  excluirLembrete,
} from './lembrete-service'

const mockLembrete = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000002',
  titulo: 'Pagar boleto',
  descricao: 'Conta de luz',
  dataLembrete: '2026-06-15',
  prioridade: 'MEDIA' as const,
  concluido: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: null,
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('listarLembretes', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockLembrete])

    const result = await listarLembretes()

    expect(apiFetch).toHaveBeenCalledWith('/api/lembretes')
    expect(result).toEqual([mockLembrete])
  })
})

describe('buscarLembrete', () => {
  it('chama apiFetch com id no path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)

    const result = await buscarLembrete(mockLembrete.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`)
    expect(result).toEqual(mockLembrete)
  })
})

describe('criarLembrete', () => {
  it('chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)

    const payload = {
      titulo: 'Pagar boleto',
      descricao: 'Conta de luz',
      dataLembrete: '2026-06-15',
      prioridade: 'MEDIA' as const,
      concluido: false,
    }
    const result = await criarLembrete(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/lembretes', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockLembrete)
  })
})

describe('atualizarLembrete', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)

    const payload = {
      titulo: 'Atualizado',
      descricao: null,
      dataLembrete: '2026-07-01',
      prioridade: 'ALTA' as const,
      concluido: true,
    }
    const result = await atualizarLembrete(mockLembrete.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockLembrete)
  })
})

describe('excluirLembrete', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await excluirLembrete(mockLembrete.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`, {
      method: 'DELETE',
    })
  })
})
