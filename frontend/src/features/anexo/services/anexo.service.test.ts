import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
  apiFetchMultipart: vi.fn(),
}))

import { apiFetch, apiFetchMultipart } from '@/services/api-client'
import { anexoService } from './anexo.service'
import type { Anexo } from '../types/anexo'

const mockAnexo: Anexo = {
  id: '00000000-0000-0000-0000-000000000001',
  nome: 'comprovante.pdf',
  tipoConteudo: 'application/pdf',
  tamanho: 2048,
  entidadeTipo: 'anotacao',
  entidadeId: '00000000-0000-0000-0000-000000000002',
  criadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('anexoService', () => {
  it('listarPorEntidade() chama apiFetch com query string correta', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockAnexo])

    const result = await anexoService.listarPorEntidade(
      'anotacao',
      '00000000-0000-0000-0000-000000000002',
    )

    expect(apiFetch).toHaveBeenCalledWith(
      '/api/anexos?entidadeTipo=anotacao&entidadeId=00000000-0000-0000-0000-000000000002',
    )
    expect(result).toEqual([mockAnexo])
  })

  it('upload() chama apiFetchMultipart com FormData contendo arquivo, entidadeTipo e entidadeId', async () => {
    vi.mocked(apiFetchMultipart).mockResolvedValue(mockAnexo)

    const file = new File(['conteudo'], 'comprovante.pdf', { type: 'application/pdf' })
    const result = await anexoService.upload('anotacao', mockAnexo.entidadeId, file)

    expect(apiFetchMultipart).toHaveBeenCalledTimes(1)
    const [path, formData] = vi.mocked(apiFetchMultipart).mock.calls[0]
    expect(path).toBe('/api/anexos')
    expect(formData).toBeInstanceOf(FormData)
    expect((formData as FormData).get('entidadeTipo')).toBe('anotacao')
    expect((formData as FormData).get('entidadeId')).toBe(mockAnexo.entidadeId)
    expect((formData as FormData).get('arquivo')).toBe(file)
    expect(result).toEqual(mockAnexo)
  })

  it('remover() chama apiFetch com DELETE no id correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await anexoService.remover(mockAnexo.id)

    expect(apiFetch).toHaveBeenCalledWith(`/api/anexos/${mockAnexo.id}`, {
      method: 'DELETE',
    })
  })

  it('downloadUrl() retorna o path do endpoint de download', () => {
    expect(anexoService.downloadUrl(mockAnexo.id)).toBe(
      `/api/anexos/${mockAnexo.id}/download`,
    )
  })
})
