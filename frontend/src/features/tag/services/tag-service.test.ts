import { describe, it, expect, vi, afterEach } from 'vitest'
import { ApiError } from '@/shared/types/api'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { listarTags, criarTag, atualizarTag, deletarTag } from './tag-service'

const mockTag = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  nome: 'Essencial',
  cor: '#FF0000',
  criadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('tag-service', () => {
  describe('listarTags()', () => {
    it('chama apiFetch com path correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue([mockTag])

      const result = await listarTags()

      expect(apiFetch).toHaveBeenCalledWith('/api/tags')
      expect(result).toEqual([mockTag])
    })

    it('retorna lista vazia quando nao ha tags', async () => {
      vi.mocked(apiFetch).mockResolvedValue([])

      const result = await listarTags()

      expect(result).toEqual([])
    })

    it('propaga erro do apiFetch', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(401, 'Nao autorizado'))

      await expect(listarTags()).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('criarTag()', () => {
    it('chama apiFetch com POST e payload correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue(mockTag)

      const payload = { nome: 'Essencial', cor: '#FF0000' }
      const result = await criarTag(payload)

      expect(apiFetch).toHaveBeenCalledWith('/api/tags', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      expect(result).toEqual(mockTag)
    })

    it('chama apiFetch sem cor quando cor e omitida', async () => {
      vi.mocked(apiFetch).mockResolvedValue({ ...mockTag, cor: undefined })

      const payload = { nome: 'Sem cor' }
      await criarTag(payload)

      expect(apiFetch).toHaveBeenCalledWith('/api/tags', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
    })

    it('propaga erro do apiFetch ao criar', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(400, 'Nome ja existe'))

      await expect(criarTag({ nome: 'Duplicada' })).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('atualizarTag()', () => {
    it('chama apiFetch com PUT e id correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue({ ...mockTag, nome: 'Atualizada' })

      const result = await atualizarTag(mockTag.id, { nome: 'Atualizada' })

      expect(apiFetch).toHaveBeenCalledWith(`/api/tags/${mockTag.id}`, {
        method: 'PUT',
        body: JSON.stringify({ nome: 'Atualizada' }),
      })
      expect(result.nome).toBe('Atualizada')
    })

    it('propaga erro 404 ao atualizar tag inexistente', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(404, 'Tag nao encontrada'))

      await expect(atualizarTag('id-inexistente', { nome: 'X' })).rejects.toBeInstanceOf(ApiError)
    })
  })

  describe('deletarTag()', () => {
    it('chama apiFetch com DELETE e id correto', async () => {
      vi.mocked(apiFetch).mockResolvedValue(undefined)

      await deletarTag(mockTag.id)

      expect(apiFetch).toHaveBeenCalledWith(`/api/tags/${mockTag.id}`, {
        method: 'DELETE',
      })
    })

    it('propaga erro 404 ao deletar tag inexistente', async () => {
      vi.mocked(apiFetch).mockRejectedValue(new ApiError(404, 'Tag nao encontrada'))

      await expect(deletarTag('id-inexistente')).rejects.toBeInstanceOf(ApiError)
    })
  })
})
