export type TipoLimite = 'DIARIO' | 'SEMANAL' | 'MENSAL' | 'ANUAL'

export interface Limite {
  id: string
  userId: string
  nome: string
  tipo: TipoLimite
  valor: number
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarLimitePayload {
  nome: string
  tipo: TipoLimite
  valor: number
}

export interface AtualizarLimitePayload {
  nome: string
  tipo: TipoLimite
  valor: number
}
