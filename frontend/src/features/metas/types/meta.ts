export interface ValorMonetario {
  valor: number
  moeda: string
}

export type StatusMeta = 'EM_ANDAMENTO' | 'CONCLUIDA' | 'CANCELADA'

export interface Meta {
  id: string
  nome: string
  valorAlvo: ValorMonetario
  valorAtual: ValorMonetario
  prazo: string
  status: StatusMeta
  atrasada: boolean
  percentualConcluido: number
  criadoEm: string
  atualizadoEm: string
}

export interface CriarMetaPayload {
  nome: string
  valorAlvoValor: number
  valorAlvoMoeda: string
  prazo: string
}

export interface RegistrarDepositoPayload {
  valor: number
  moeda: string
}
