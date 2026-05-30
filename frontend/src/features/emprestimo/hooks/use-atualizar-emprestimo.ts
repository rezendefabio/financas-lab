import { useMutation, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'
import type { AtualizarEmprestimoPayload } from '../types/emprestimo'

export function useAtualizarEmprestimo(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AtualizarEmprestimoPayload) =>
      emprestimoService.atualizar(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      await queryClient.invalidateQueries({ queryKey: ['emprestimos', id] })
    },
  })
}
