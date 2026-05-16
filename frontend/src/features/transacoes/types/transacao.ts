export type StatusTransacao = 'PENDING' | 'CLEARED' | 'SCHEDULED' | 'CANCELLED'

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
  status: StatusTransacao
  payeeId: string | null
  tagIds: string[]
  transferGroupId: string | null
}

export interface CriarTransacaoRequest {
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: number
  moeda: string
  data: string
  descricao: string
  contaId: string
  contaDestinoId?: string
  categoriaId?: string
  status?: StatusTransacao
  payeeId?: string
  tagIds?: string[]
}
