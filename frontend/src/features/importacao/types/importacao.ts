export interface ImportacaoJobResponse {
  jobExecutionId: number
  status: string
}

export interface AnaliseItemResponse {
  linha: number
  linhaCsvOriginal: string
  tipo: string
  valor: number
  moeda: string
  data: string
  descricao: string
  contaId: string
  possivelDuplicata: boolean
  transacaoExistenteId: string | null
}

export interface AnaliseImportacaoResponse {
  totalLinhas: number
  linhasValidas: number
  possivelDuplicatas: number
  errosParsing: number
  itens: AnaliseItemResponse[]
  erros: Array<{ linha: number; motivo: string }>
}
