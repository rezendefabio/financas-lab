import { apiFetch } from '@/services/api-client'
import type {
  EmprestimoResponse,
  CriarEmprestimoPayload,
  AtualizarEmprestimoPayload,
} from '../types/emprestimo'

export const emprestimoService = {
  listar: (): Promise<EmprestimoResponse[]> =>
    apiFetch<EmprestimoResponse[]>('/api/emprestimos'),
  // O backend expoe apenas listagem (List<>); a busca por id deriva da lista.
  buscarPorId: async (id: string): Promise<EmprestimoResponse | undefined> => {
    const todos = await apiFetch<EmprestimoResponse[]>('/api/emprestimos')
    return todos.find((e) => e.id === id)
  },
  criar: (payload: CriarEmprestimoPayload) =>
    apiFetch<EmprestimoResponse>('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  atualizar: (id: string, payload: AtualizarEmprestimoPayload) =>
    apiFetch<EmprestimoResponse>(`/api/emprestimos/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  deletar: (id: string) =>
    apiFetch<void>(`/api/emprestimos/${id}`, { method: 'DELETE' }),
}
