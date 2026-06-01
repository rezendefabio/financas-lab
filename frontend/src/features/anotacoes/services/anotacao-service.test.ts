import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { anotacaoService } from './anotacao-service'
import type { Anotacao } from '../types/anotacao'

const mockAnotacao: Anotacao = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000002',
  titulo: 'Pagar fatura',
  conteudo: null,
  tipo: 'LEMBRETE',
  prioridade: 'MEDIA',
  valorMontante: null,
  valorMoeda: null,
  dataReferencia: null,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('anotacaoService', () => {
  it('listar() chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockAnotacao])

    const result = await anotacaoService.listar()

    expect(apiFetch).toHaveBeenCalledWith('/api/anotacoes')
    expect(result).toEqual([mockAnotacao])
  })

  it('buscarPorId() chama apiFetch com path correto para o id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAnotacao)

    const result = await anotacaoService.buscarPorId(mockAnotacao.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/anotacoes/${mockAnotacao.id}`)
    expect(result).toEqual(mockAnotacao)
  })

  it('criar() chama apiFetch com POST e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAnotacao)

    const payload = {
      titulo: 'Pagar fatura',
      tipo: 'LEMBRETE' as const,
      prioridade: 'MEDIA' as const,
    }

    const result = await anotacaoService.criar(payload)

    expect(apiFetch).toHaveBeenCalledWith('/api/anotacoes', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockAnotacao)
  })

  it('atualizar() chama apiFetch com PUT e payload correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockAnotacao)

    const payload = {
      titulo: 'Titulo atualizado',
      tipo: 'ALERTA' as const,
      prioridade: 'ALTA' as const,
    }

    const result = await anotacaoService.atualizar(mockAnotacao.id, payload)

    expect(apiFetch).toHaveBeenCalledWith(`/api/anotacoes/${mockAnotacao.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    expect(result).toEqual(mockAnotacao)
  })

  it('deletar() chama apiFetch com DELETE e id correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await anotacaoService.deletar(mockAnotacao.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/anotacoes/${mockAnotacao.id}`, {
      method: 'DELETE',
    })
  })
})
