import { apiFetchMultipart } from '@/services/api-client'
import { getToken } from '@/shared/lib/auth'
import type { ImportacaoJobResponse } from '../types/importacao'

// Mesma resolucao de base URL usada por api-client.ts -- mantida em sincronia.
const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

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

  downloadModelo: async (): Promise<void> => {
    const token = getToken()
    // fetch direto: precisamos do Response bruto para criar o Blob.
    // apiFetch/apiFetchMultipart retornam JSON e nao servem para download de arquivo.
    // eslint-disable-next-line no-restricted-globals
    const response = await fetch(
      `${API_BASE}/api/jobs/importacao-csv-transacoes/csv/modelo`,
      { headers: token ? { Authorization: `Bearer ${token}` } : {} },
    )
    if (!response.ok) throw new Error('Erro ao baixar o modelo CSV.')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'modelo-importacao-transacoes.csv'
    a.click()
    URL.revokeObjectURL(url)
  },
}
