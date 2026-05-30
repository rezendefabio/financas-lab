import { useQuery } from '@tanstack/react-query'
import { emprestimoService } from '../services/emprestimo-service'

export function useEmprestimos() {
  return useQuery({
    queryKey: ['emprestimos'],
    queryFn: () => emprestimoService.listar(),
  })
}
