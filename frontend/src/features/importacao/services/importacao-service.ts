import { apiFetchMultipart } from '@/services/api-client'
import type { ImportacaoJobResponse } from '../types/importacao'

/**
 * Servico de importacao de transacoes via CSV.
 *
 * Usa apiFetchMultipart porque o upload exige que o browser defina o header
 * Content-Type com o boundary -- apiFetch forca application/json.
 */
export const importacaoService = {
  importarCsv: async (arquivo: File): Promise<ImportacaoJobResponse> => {
    const formData = new FormData()
    formData.append('arquivo', arquivo)
    return apiFetchMultipart<ImportacaoJobResponse>(
      '/api/jobs/importacao-csv-transacoes',
      formData,
    )
  },
}
