export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface FaturaResponse {
  id: string
  contaId: string
  nome: string
  dataVencimento: string
  dataFechamento: string | null
  valorTotal: ValorMonetario | null
  paga: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarFaturaRequest {
  contaId: string
  nome: string
  dataVencimento: string
  dataFechamento?: string
  valorTotalValor?: number
  valorTotalMoeda?: string
}

export interface AtualizarFaturaRequest {
  nome: string
  dataVencimento: string
  dataFechamento?: string
  valorTotalValor?: number
  valorTotalMoeda?: string
}
