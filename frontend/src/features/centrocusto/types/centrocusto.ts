export interface CentroCusto {
  id: string
  userId: string
  nome: string
  descricao?: string | null
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarCentroCustoPayload {
  nome: string
  descricao?: string
}

export interface AtualizarCentroCustoPayload {
  nome: string
  descricao?: string
}
