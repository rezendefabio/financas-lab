import { useQuery } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'

export function useEmprestimo(id: string | undefined) {
  return useQuery({
    queryKey: ['emprestimos', id],
    queryFn: () => emprestimoService.buscarPorId(id as string),
    enabled: Boolean(id),
  })
}
