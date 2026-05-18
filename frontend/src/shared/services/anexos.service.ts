import { apiFetch, apiFetchMultipart } from '@/services/api-client'
import type { Anexo } from '@/shared/types/anexo'

/**
 * Servico transversal de anexos. Anexos pertencem logicamente a qualquer
 * entidade do sistema (entidadeTipo + entidadeId), por isso vive em shared/.
 */
export const anexosService = {
  upload: (arquivo: File, entidadeTipo: string, entidadeId: string): Promise<Anexo> => {
    const formData = new FormData()
    formData.append('arquivo', arquivo)
    formData.append('entidadeTipo', entidadeTipo)
    formData.append('entidadeId', entidadeId)
    return apiFetchMultipart<Anexo>('/api/anexos', formData)
  },

  listar: (entidadeTipo: string, entidadeId: string): Promise<Anexo[]> => {
    const qs = new URLSearchParams({ entidadeTipo, entidadeId })
    return apiFetch<Anexo[]>(`/api/anexos?${qs}`)
  },

  remover: (id: string): Promise<void> =>
    apiFetch<void>(`/api/anexos/${id}`, { method: 'DELETE' }),

  /** URL absoluta para o endpoint de download (302 -> URL temporaria do MinIO). */
  urlDownload: (id: string): string => {
    const base = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'
    return `${base}/api/anexos/${id}/download`
  },
}
