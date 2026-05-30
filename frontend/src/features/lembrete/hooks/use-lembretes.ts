import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  listarLembretes,
  buscarLembrete,
  criarLembrete,
  atualizarLembrete,
  excluirLembrete,
} from '../services/lembrete-service'
import type {
  Lembrete,
  CriarLembretePayload,
  AtualizarLembretePayload,
} from '../types/lembrete'

const LIST_KEY = ['lembretes'] as const

export function useLembretes() {
  return useQuery<Lembrete[]>({
    queryKey: LIST_KEY,
    queryFn: listarLembretes,
  })
}

export function useLembrete(id: string | undefined) {
  return useQuery<Lembrete>({
    queryKey: ['lembrete', id],
    queryFn: () => buscarLembrete(id as string),
    enabled: Boolean(id),
  })
}

export function useCriarLembrete() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarLembretePayload) => criarLembrete(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: LIST_KEY })
    },
  })
}

export function useAtualizarLembrete(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarLembretePayload) => atualizarLembrete(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: LIST_KEY })
      await queryClient.invalidateQueries({ queryKey: ['lembrete', id] })
    },
  })
}

export function useExcluirLembrete() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => excluirLembrete(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: LIST_KEY })
    },
  })
}
