import { apiFetch } from '@/services/api-client'
import type {
  Emprestimo,
  CriarEmprestimoPayload,
  AtualizarEmprestimoPayload,
} from '../types/emprestimo'

export const emprestimoService = {
  listar: () => apiFetch<Emprestimo[]>('/api/emprestimos'),
  buscar: (id: string) => apiFetch<Emprestimo>(`/api/emprestimos/${id}`),
  criar: (payload: CriarEmprestimoPayload) =>
    apiFetch<Emprestimo>('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  atualizar: (id: string, payload: AtualizarEmprestimoPayload) =>
    apiFetch<Emprestimo>(`/api/emprestimos/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  remover: (id: string) =>
    apiFetch<void>(`/api/emprestimos/${id}`, {
      method: 'DELETE',
    }),
}
