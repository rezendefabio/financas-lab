import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { assinaturaService } from './assinatura-service'
import type { Assinatura } from '../types/assinatura'

const mockAssinatura: Assinatura = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  nome: 'Netflix',
  tipo: 'STREAMING',
  valorMensal: { valor: 29.9, moeda: 'BRL' },
  dataRenovacao: '2026-06-15',
  ativa: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('assinaturaService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockAssinatura])
    const result = await assinaturaService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/assinaturas')
    expect(result).toEqual([mockAssinatura])
  })
})

describe('assinaturaService.buscar', () => {
  it('chama apiFetch com path do id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAssinatura)
    await assinaturaService.buscar(mockAssinatura.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/assinaturas/${mockAssinatura.id}`)
  })
})

describe('assinaturaService.criar', () => {
  it('chama apiFetch com POST e payload serializado', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAssinatura)
    const payload = {
      nome: 'Netflix',
      tipo: 'STREAMING' as const,
      valorMensal: 29.9,
      moeda: 'BRL',
      dataRenovacao: '2026-06-15',
    }
    await assinaturaService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/assinaturas', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('assinaturaService.atualizar', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAssinatura)
    const payload = {
      nome: 'Spotify',
      tipo: 'OUTROS' as const,
      valorMensal: 49.9,
      moeda: 'BRL',
      dataRenovacao: '2026-07-01',
      ativa: false,
    }
    await assinaturaService.atualizar(mockAssinatura.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/assinaturas/${mockAssinatura.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('assinaturaService.remover', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await assinaturaService.remover(mockAssinatura.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/assinaturas/${mockAssinatura.id}`, {
      method: 'DELETE',
    })
  })
})
