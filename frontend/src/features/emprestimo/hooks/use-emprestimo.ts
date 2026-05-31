'use client'

import { useQuery } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'

/** Lista todos os emprestimos do usuario (client-side). */
export function useEmprestimos() {
  return useQuery({
    queryKey: ['emprestimos'],
    queryFn: emprestimoService.listar,
  })
}

/** Busca um emprestimo por id (pagina de edicao). */
export function useEmprestimo(id: string) {
  return useQuery({
    queryKey: ['emprestimos', id],
    queryFn: () => emprestimoService.buscarPorId(id),
    enabled: Boolean(id),
  })
}
