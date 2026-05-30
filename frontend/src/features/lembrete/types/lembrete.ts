export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA'

export interface LembreteResponse {
  id: string
  titulo: string
  descricao: string | null
  dataLembrete: string
  prioridade: Prioridade
  concluido: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarLembretePayload {
  titulo: string
  descricao?: string
  dataLembrete: string
  prioridade: Prioridade
}

export interface AtualizarLembretePayload {
  titulo: string
  descricao?: string
  dataLembrete: string
  prioridade: Prioridade
  concluido: boolean
}
