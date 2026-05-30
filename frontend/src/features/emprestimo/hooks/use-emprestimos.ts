'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'
import type {
  AtualizarEmprestimoPayload,
  CriarEmprestimoPayload,
  EmprestimoResponse,
} from '../types'

const QUERY_KEY = ['emprestimos'] as const

export function useEmprestimos() {
  return useQuery<EmprestimoResponse[]>({
    queryKey: QUERY_KEY,
    queryFn: emprestimoService.listar,
  })
}

export function useEmprestimo(id: string | undefined) {
  return useQuery<EmprestimoResponse>({
    queryKey: ['emprestimos', id],
    queryFn: () => emprestimoService.buscarPorId(id as string),
    enabled: !!id,
  })
}

export function useCriarEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarEmprestimoPayload) =>
      emprestimoService.criar(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY })
    },
  })
}

export function useAtualizarEmprestimo(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarEmprestimoPayload) =>
      emprestimoService.atualizar(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: ['emprestimos', id] })
    },
  })
}

export function useDeletarEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => emprestimoService.deletar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY })
    },
  })
}
