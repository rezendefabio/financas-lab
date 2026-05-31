import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assinaturaService } from '../services/assinatura-service'
import type {
  CriarAssinaturaPayload,
  AtualizarAssinaturaPayload,
} from '../types/assinatura'

export function useAssinaturas() {
  return useQuery({
    queryKey: ['assinaturas'],
    queryFn: () => assinaturaService.listar(),
  })
}

export function useAssinatura(id: string | undefined) {
  return useQuery({
    queryKey: ['assinatura', id],
    queryFn: () => assinaturaService.buscar(id as string),
    enabled: !!id,
  })
}

export function useCriarAssinatura() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarAssinaturaPayload) => assinaturaService.criar(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assinaturas'] }),
  })
}

export function useAtualizarAssinatura(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarAssinaturaPayload) =>
      assinaturaService.atualizar(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['assinaturas'] })
      await queryClient.invalidateQueries({ queryKey: ['assinatura', id] })
    },
  })
}

export function useRemoverAssinatura() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => assinaturaService.remover(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assinaturas'] }),
  })
}
