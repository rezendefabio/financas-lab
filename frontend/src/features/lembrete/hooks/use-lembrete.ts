'use client'

import { useQuery } from '@tanstack/react-query'
import { lembreteService } from '../services/lembrete-service'

export function useLembretes() {
  return useQuery({
    queryKey: ['lembretes'],
    queryFn: () => lembreteService.listar(),
  })
}

export function useLembrete(id: string | undefined) {
  return useQuery({
    queryKey: ['lembretes', id],
    queryFn: () => lembreteService.buscarPorId(id as string),
    enabled: !!id,
  })
}
