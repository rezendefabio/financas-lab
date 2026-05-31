'use client'
/**
 * Hooks de notificacoes -- agora servidas pelo backend (bounded context
 * notificacao). A lista vem do GET /api/notificacoes (ja reconciliado e
 * filtrado para nao-descartadas). O descarte e PERSISTIDO via PATCH, entao
 * sobrevive entre logins -- corrige o bug de a notificacao reaparecer.
 */
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificacoesService } from '../services/notificacao-service'
import type { Notificacao } from '../types/notificacao'

export function useNotificacoes(): {
  notificacoes: Notificacao[]
  isLoading: boolean
} {
  const { data, isLoading } = useQuery({
    queryKey: ['notificacoes'],
    queryFn: () => notificacoesService.listar(),
  })
  return { notificacoes: data ?? [], isLoading }
}

export function useDescartarNotificacao() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificacoesService.descartar(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notificacoes'] }),
  })
}
