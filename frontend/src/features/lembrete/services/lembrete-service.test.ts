import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { lembreteService } from './lembrete-service'
import type { LembreteResponse } from '../types/lembrete'

const mockLembrete: LembreteResponse = {
  id: '00000000-0000-0000-0000-000000000001',
  titulo: 'Pagar conta',
  descricao: 'Conta de luz',
  dataLembrete: '2026-06-15',
  prioridade: 'MEDIA',
  concluido: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('lembreteService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockLembrete])
    const result = await lembreteService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/lembretes')
    expect(result).toEqual([mockLembrete])
  })
})

describe('lembreteService.buscarPorId', () => {
  it('chama apiFetch com GET no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)
    await lembreteService.buscarPorId(mockLembrete.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`)
  })
})

describe('lembreteService.criar', () => {
  it('chama apiFetch com POST e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)
    const payload = {
      titulo: 'Pagar conta',
      descricao: 'Conta de luz',
      dataLembrete: '2026-06-15',
      prioridade: 'MEDIA' as const,
    }
    await lembreteService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/lembretes', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('lembreteService.atualizar', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockLembrete)
    const payload = {
      titulo: 'Atualizado',
      descricao: 'Nova desc',
      dataLembrete: '2026-06-20',
      prioridade: 'ALTA' as const,
      concluido: true,
    }
    await lembreteService.atualizar(mockLembrete.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('lembreteService.deletar', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await lembreteService.deletar(mockLembrete.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/lembretes/${mockLembrete.id}`, {
      method: 'DELETE',
    })
  })
})
