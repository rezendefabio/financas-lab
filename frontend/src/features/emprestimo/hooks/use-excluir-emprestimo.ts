import { useMutation, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'

export function useExcluirEmprestimo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => emprestimoService.excluir(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
    },
  })
}
