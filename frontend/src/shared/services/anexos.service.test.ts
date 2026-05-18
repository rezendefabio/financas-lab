import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { anexosService } from './anexos.service'
import * as apiClient from '@/services/api-client'

describe('anexosService', () => {
  beforeEach(() => {
    vi.spyOn(apiClient, 'apiFetch')
    vi.spyOn(apiClient, 'apiFetchMultipart')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('upload monta FormData com arquivo, entidadeTipo e entidadeId', async () => {
    vi.mocked(apiClient.apiFetchMultipart).mockResolvedValue({} as never)
    const arquivo = new File(['conteudo'], 'doc.pdf', { type: 'application/pdf' })

    await anexosService.upload(arquivo, 'TRANSACAO', 'id-123')

    const [path, formData] = vi.mocked(apiClient.apiFetchMultipart).mock.calls[0]
    expect(path).toBe('/api/anexos')
    expect((formData as FormData).get('arquivo')).toBe(arquivo)
    expect((formData as FormData).get('entidadeTipo')).toBe('TRANSACAO')
    expect((formData as FormData).get('entidadeId')).toBe('id-123')
  })

  it('listar chama endpoint com query string de entidadeTipo e entidadeId', async () => {
    vi.mocked(apiClient.apiFetch).mockResolvedValue([] as never)

    await anexosService.listar('CONTA', 'id-456')

    expect(apiClient.apiFetch).toHaveBeenCalledWith(
      '/api/anexos?entidadeTipo=CONTA&entidadeId=id-456',
    )
  })

  it('remover chama DELETE no endpoint do anexo', async () => {
    vi.mocked(apiClient.apiFetch).mockResolvedValue(undefined as never)

    await anexosService.remover('id-789')

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/anexos/id-789', {
      method: 'DELETE',
    })
  })

  it('urlDownload monta URL absoluta para o endpoint de download', () => {
    expect(anexosService.urlDownload('id-abc')).toContain('/api/anexos/id-abc/download')
  })
})
