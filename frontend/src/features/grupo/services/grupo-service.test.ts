import { describe, it, expect, vi, afterEach } from 'vitest'
import { ApiError } from '@/shared/types/api'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { listarGrupos, criarGrupo, atualizarGrupo, deletarGrupo } from './grupo-service'

const mockGrupo = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  nome: 'Viagem Europa',
  descricao: 'Gastos da viagem',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('grupo-service', () => {
  describe('listarGrupos()', () => {
    it('chama apiFetch com path correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue([mockGrupo])

      const result = await listarGrupos()

      expect(apiFetch).toHaveBeenCalledWith('/api/grupos')
      expect(result).toEqual([mockGrupo])
    })

    it('retorna lista vazia quando nao ha grupos', async () => {
      vi.mocked(apiFetch).mockResolvedValue([])

      const result = await listarGrupos()

      expect(result).toEqual([])
    })

    it('propaga erro do apiFetch', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(401, 'Nao autorizado'))

      await expect(listarGrupos()).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('criarGrupo()', () => {
    it('chama apiFetch com POST e payload correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue(mockGrupo)

      const payload = { nome: 'Viagem Europa', descricao: 'Gastos da viagem' }
      const result = await criarGrupo(payload)

      expect(apiFetch).toHaveBeenCalledWith('/api/grupos', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      expect(result).toEqual(mockGrupo)
    })

    it('aceita payload sem descricao', async () => {
      vi.mocked(apiFetch).mockResolvedValue(mockGrupo)

      await criarGrupo({ nome: 'Casa Nova' })

      expect(apiFetch).toHaveBeenCalledWith('/api/grupos', {
        method: 'POST',
        body: JSON.stringify({ nome: 'Casa Nova' }),
      })
    })

    it('propaga erro do apiFetch ao criar', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(400, 'Nome invalido'))

      await expect(criarGrupo({ nome: '' })).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('atualizarGrupo()', () => {
    it('chama apiFetch com PUT e id correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue({ ...mockGrupo, nome: 'Atualizado' })

      const result = await atualizarGrupo(mockGrupo.id, { nome: 'Atualizado' })

      expect(apiFetch).toHaveBeenCalledWith(`/api/grupos/${mockGrupo.id}`, {
        method: 'PUT',
        body: JSON.stringify({ nome: 'Atualizado' }),
      })
      expect(result.nome).toBe('Atualizado')
    })

    it('propaga erro 404 ao atualizar grupo inexistente', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(404, 'Grupo nao encontrado'))

      await expect(atualizarGrupo('id-inexistente', { nome: 'X' })).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('deletarGrupo()', () => {
    it('chama apiFetch com DELETE e id correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue(undefined)

      await deletarGrupo(mockGrupo.id)

      expect(apiFetch).toHaveBeenCalledWith(`/api/grupos/${mockGrupo.id}`, {
        method: 'DELETE',
      })
    })

    it('propaga erro 404 ao deletar grupo inexistente', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(404, 'Grupo nao encontrado'))

      await expect(deletarGrupo('id-inexistente')).rejects.toBeInstanceOf(ApiError)
    })
  })
})
