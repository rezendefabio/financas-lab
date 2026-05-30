import { apiFetch } from '@/services/api-client'
import type {
  AtualizarEmprestimoPayload,
  CriarEmprestimoPayload,
  EmprestimoResponse,
} from '../types/emprestimo'

export const emprestimoService = {
  listar: (): Promise<EmprestimoResponse[]> =>
    apiFetch<EmprestimoResponse[]>('/api/emprestimos'),

  buscarPorId: (id: string): Promise<EmprestimoResponse> =>
    apiFetch<EmprestimoResponse>(`/api/emprestimos/${id}`),

  criar: (payload: CriarEmprestimoPayload): Promise<EmprestimoResponse> =>
    apiFetch<EmprestimoResponse>('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  atualizar: (
    id: string,
    payload: AtualizarEmprestimoPayload,
  ): Promise<EmprestimoResponse> =>
    apiFetch<EmprestimoResponse>(`/api/emprestimos/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  excluir: (id: string): Promise<void> =>
    apiFetch<void>(`/api/emprestimos/${id}`, { method: 'DELETE' }),
}
