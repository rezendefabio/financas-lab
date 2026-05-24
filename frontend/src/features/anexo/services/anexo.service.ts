import { apiFetch, apiFetchMultipart } from '@/services/api-client'
import type { Anexo } from '../types/anexo'

/**
 * Servico do bounded context `anexo`. Encapsula chamadas REST para
 * listagem, upload, exclusao e geracao de URL de download.
 *
 * O download e feito via `<a href={downloadUrl(id)} target="_blank">`
 * porque o endpoint do backend retorna um redirect 302 para uma URL
 * presignada -- nao um blob.
 */
export const anexoService = {
  listarPorEntidade: (entidadeTipo: string, entidadeId: string) =>
    apiFetch<Anexo[]>(
      `/api/anexos?entidadeTipo=${encodeURIComponent(entidadeTipo)}&entidadeId=${encodeURIComponent(entidadeId)}`,
    ),

  // upload multipart -- usa apiFetchMultipart (helper central de api-client.ts)
  // em vez de fetch direto, mantendo a regra B3 (todo fetch concentrado em api-client.ts)
  upload: (entidadeTipo: string, entidadeId: string, arquivo: File) => {
    const formData = new FormData()
    formData.append('arquivo', arquivo)
    formData.append('entidadeTipo', entidadeTipo)
    formData.append('entidadeId', entidadeId)
    return apiFetchMultipart<Anexo>('/api/anexos', formData)
  },

  remover: (id: string) =>
    apiFetch<void>(`/api/anexos/${id}`, { method: 'DELETE' }),

  downloadUrl: (id: string) => `/api/anexos/${id}/download`,
}
