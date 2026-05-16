import { apiFetch } from '@/services/api-client'
import type { Anotacao, CriarAnotacaoRequest, AtualizarAnotacaoRequest } from '../types/anotacao'

export const anotacaoService = {
  listar: () => apiFetch<Anotacao[]>('/api/anotacoes'),
  buscarPorId: (id: string) => apiFetch<Anotacao>(`/api/anotacoes/${id}`),
  criar: (req: CriarAnotacaoRequest) =>
    apiFetch<Anotacao>('/api/anotacoes', {
      method: 'POST',
      body: JSON.stringify(req),
    }),
  atualizar: (id: string, req: AtualizarAnotacaoRequest) =>
    apiFetch<Anotacao>(`/api/anotacoes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(req),
    }),
  deletar: (id: string) =>
    apiFetch<void>(`/api/anotacoes/${id}`, { method: 'DELETE' }),
}
