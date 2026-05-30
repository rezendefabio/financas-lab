import { useMutation, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'
import type { CriarEmprestimoPayload } from '../types/emprestimo'

export function useCriarEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CriarEmprestimoPayload) => emprestimoService.criar(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
    },
  })
}
