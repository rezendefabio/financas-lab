import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimosService } from '../services/emprestimos-service'
import type {
  CriarEmprestimoPayload,
  AtualizarEmprestimoPayload,
  Emprestimo,
} from '../types/emprestimo'

export function useEmprestimos() {
  return useQuery({
    queryKey: ['emprestimos'],
    queryFn: () => emprestimosService.listar(),
  })
}

/**
 * Carrega um emprestimo unico. Como o backend nao expoe GET /{id}, derivamos o
 * registro a partir da listagem (cadastro client-side).
 */
export function useEmprestimo(id: string | undefined) {
  return useQuery({
    queryKey: ['emprestimos', id],
    queryFn: async (): Promise<Emprestimo | undefined> => {
      const todos = await emprestimosService.listar()
      return todos.find((e) => e.id === id)
    },
    enabled: !!id,
  })
}

export function useCriarEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarEmprestimoPayload) => emprestimosService.criar(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['emprestimos'] }),
  })
}

export function useAtualizarEmprestimo(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarEmprestimoPayload) =>
      emprestimosService.atualizar(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['emprestimos'] }),
  })
}

export function useRemoverEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => emprestimosService.remover(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['emprestimos'] }),
  })
}
