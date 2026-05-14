export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface Orcamento {
  id: string
  categoriaId: string
  valorLimite: ValorMonetario
  mesAno: string
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export type OrcamentoStatus = 'ABAIXO' | 'ATENCAO' | 'ATINGIDO' | 'EXCEDIDO'

export interface Progresso {
  orcamentoId: string
  categoriaId: string
  mesAno: string
  valorLimite: ValorMonetario
  totalGasto: ValorMonetario
  percentualUtilizado: number
  status: OrcamentoStatus
}

export interface CriarOrcamentoPayload {
  categoriaId: string
  valorLimiteValor: number
  valorLimiteMoeda: string
  mesAno: string
}
