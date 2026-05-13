import { apiFetch } from './api-client'
import type { Transacao } from '@/types/transacao'

interface CriarTransacaoRequest {
  tipo: string
  valorValor: number
  valorMoeda: string
  data: string
  descricao: string
  contaId: string
  contaDestinoId?: string
  categoriaId?: string
}

interface ListarTransacoesParams {
  contaId?: string
  dataInicio?: string
  dataFim?: string
  tipo?: string
  page?: number
  size?: number
}

export const transacoesService = {
  listar: (params?: ListarTransacoesParams) => {
    const qs = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined) qs.set(k, String(v))
      })
    }
    const query = qs.toString() ? `?${qs}` : ''
    return apiFetch<{ content: Transacao[]; totalElements: number }>(`/api/transacoes${query}`)
  },
  criar: (data: CriarTransacaoRequest) =>
    apiFetch<Transacao>('/api/transacoes', { method: 'POST', body: JSON.stringify(data) }),
}
