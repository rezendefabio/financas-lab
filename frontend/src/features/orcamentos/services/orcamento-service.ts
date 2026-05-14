import { apiFetch } from '@/services/api-client'
import type { Orcamento, Progresso, CriarOrcamentoPayload } from '../types/orcamento'

export const orcamentoService = {
  listar: () => apiFetch<Orcamento[]>('/api/orcamentos'),
  buscar: (id: string) => apiFetch<Orcamento>(`/api/orcamentos/${id}`),
  criar: (payload: CriarOrcamentoPayload) =>
    apiFetch<Orcamento>('/api/orcamentos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  desativar: (id: string) =>
    apiFetch<void>(`/api/orcamentos/${id}`, { method: 'DELETE' }),
  progresso: (id: string) =>
    apiFetch<Progresso>(`/api/orcamentos/${id}/progresso`),
}
