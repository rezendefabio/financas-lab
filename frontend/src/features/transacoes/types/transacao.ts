export interface Transacao {
  id: string
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: number
  moeda: string
  data: string
  descricao: string
  contaId: string
  contaDestinoId: string | null
  categoriaId: string | null
  criadoEm: string
  atualizadoEm: string
}
