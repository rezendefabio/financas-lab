import { apiFetch } from '@/services/api-client'
import type {
  LancamentoRecorrente,
  CriarLancamentoRecorrenteRequest,
} from '../types/lancamento-recorrente'

export const lancamentoRecorrenteService = {
  listar: () =>
    apiFetch<LancamentoRecorrente[]>('/api/lancamentos-recorrentes'),

  criar: (data: CriarLancamentoRecorrenteRequest) =>
    apiFetch<LancamentoRecorrente>('/api/lancamentos-recorrentes', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  buscar: (id: string) =>
    apiFetch<LancamentoRecorrente>(`/api/lancamentos-recorrentes/${id}`),

  desativar: (id: string) =>
    apiFetch<void>(`/api/lancamentos-recorrentes/${id}`, { method: 'DELETE' }),

  executar: (id: string) =>
    apiFetch<void>(`/api/lancamentos-recorrentes/${id}/execucoes`, {
      method: 'POST',
    }),
}
