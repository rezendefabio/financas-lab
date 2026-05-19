import { apiFetch } from '@/services/api-client'
import type { PageResponse } from '@/shared/hooks/useListPage'
import type {
  Transacao,
  CriarTransacaoRequest,
  StatusTransacao,
} from '../types/transacao'

/** Parametros de filtro e paginacao aceitos por `GET /api/transacoes`. */
export interface ListarTransacoesParams {
  contaId?: string
  dataInicio?: string
  dataFim?: string
  tipo?: string
  status?: StatusTransacao | string
  categoriaId?: string
  /**
   * Filtros adicionais com operador, no formato serializado
   * `campo:operador:valor,campo2:operador2:valor2` (ex: `descricao:contains:mercado`).
   */
  filtros?: string
  page?: number
  size?: number
  /** Ordenacao no formato `campo,dir` (ex: `data,desc`). */
  sort?: string
}

export const transacoesService = {
  listar: (params?: ListarTransacoesParams) => {
    const qs = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== '') qs.set(k, String(v))
      })
    }
    const query = qs.toString() ? `?${qs}` : ''
    return apiFetch<PageResponse<Transacao>>(`/api/transacoes${query}`)
  },
  criar: (data: CriarTransacaoRequest) =>
    apiFetch<Transacao>('/api/transacoes', { method: 'POST', body: JSON.stringify(data) }),
}
