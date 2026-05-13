import { ValorMonetario } from '@/features/contas/types/conta'

export interface Transacao {
  id: string
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: ValorMonetario
  data: string
  descricao: string
  contaId: string
  contaDestinoId: string | null
  categoriaId: string | null
  criadoEm: string
}
