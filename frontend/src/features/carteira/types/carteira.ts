export type TipoCarteira = 'RENDA_FIXA' | 'RENDA_VARIAVEL' | 'CRIPTOMOEDA' | 'OUTROS'

export interface CarteiraResponse {
  id: string
  contaId: string
  nome: string
  tipo: TipoCarteira
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarCarteiraRequest {
  contaId: string
  nome: string
  tipo: TipoCarteira
}

export interface AtualizarCarteiraRequest {
  nome: string
  tipo: TipoCarteira
}
