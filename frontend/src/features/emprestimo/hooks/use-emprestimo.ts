import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'
import type {
  CriarEmprestimoPayload,
  AtualizarEmprestimoPayload,
} from '../types/emprestimo'

export function useEmprestimos() {
  return useQuery({
    queryKey: ['emprestimos'],
    queryFn: () => emprestimoService.listar(),
  })
}

export function useEmprestimo(id: string | undefined) {
  return useQuery({
    queryKey: ['emprestimo', id],
    queryFn: () => emprestimoService.buscar(id as string),
    enabled: !!id,
  })
}

export function useCriarEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarEmprestimoPayload) =>
      emprestimoService.criar(payload),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['emprestimos'] }),
  })
}

export function useAtualizarEmprestimo(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarEmprestimoPayload) =>
      emprestimoService.atualizar(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      queryClient.invalidateQueries({ queryKey: ['emprestimo', id] })
    },
  })
}

export function useRemoverEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => emprestimoService.remover(id),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['emprestimos'] }),
  })
}
