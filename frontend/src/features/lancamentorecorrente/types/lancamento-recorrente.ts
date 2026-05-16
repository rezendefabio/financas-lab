export type TipoTransacao = 'RECEITA' | 'DESPESA'

export type Periodicidade =
  | 'SEMANAL'
  | 'QUINZENAL'
  | 'MENSAL'
  | 'BIMESTRAL'
  | 'TRIMESTRAL'
  | 'SEMESTRAL'
  | 'ANUAL'

export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface LancamentoRecorrente {
  id: string
  descricao: string
  tipo: TipoTransacao
  valor: ValorMonetario
  contaId: string
  categoriaId: string | null
  periodicidade: Periodicidade
  proximaOcorrencia: string
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarLancamentoRecorrenteRequest {
  descricao: string
  tipo: TipoTransacao
  valorValor: number
  valorMoeda: string
  contaId: string
  categoriaId?: string
  periodicidade: Periodicidade
  proximaOcorrencia: string
}
