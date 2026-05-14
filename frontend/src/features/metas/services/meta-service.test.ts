import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { metaService } from './meta-service'

const mockMeta = {
  id: '00000000-0000-0000-0000-000000000001',
  nome: 'Viagem Europa',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 3000, moeda: 'BRL' },
  prazo: '2027-12-31',
  status: 'EM_ANDAMENTO' as const,
  atrasada: false,
  percentualConcluido: 30,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('metaService', () => {
  it('listar() chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockMeta])

    const result = await metaService.listar()

    expect(apiFetch).toHaveBeenCalledWith('/api/metas')
    expect(result).toEqual([mockMeta])
  })

  it('buscar() chama apiFetch com path correto para o id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockMeta)

    const result = await metaService.buscar(mockMeta.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/metas/${mockMeta.id}`)
    expect(result).toEqual(mockMeta)
  })

  it('criar() chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockMeta)

    const payload = {
      nome: 'Viagem Europa',
      valorAlvoValor: 10000,
      valorAlvoMoeda: 'BRL',
      prazo: '2027-12-31',
    }

    const result = await metaService.criar(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/metas', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockMeta)
  })

  it('cancelar() chama apiFetch com DELETE e id correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await metaService.cancelar(mockMeta.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/metas/${mockMeta.id}`, {
      method: 'DELETE',
    })
  })

  it('registrarDeposito() chama apiFetch com POST no path de depositos e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockMeta)

    const payload = { valor: 500, moeda: 'BRL' }

    const result = await metaService.registrarDeposito(mockMeta.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/metas/${mockMeta.id}/depositos`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockMeta)
  })
})
