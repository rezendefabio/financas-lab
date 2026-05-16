export type TipoAnotacao = 'LEMBRETE' | 'OBSERVACAO' | 'ALERTA' | 'PLANEJAMENTO'
export type PrioridadeAnotacao = 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE'

export interface Anotacao {
  id: string
  usuarioId: string
  titulo: string
  conteudo: string | null
  tipo: TipoAnotacao
  prioridade: PrioridadeAnotacao
  valorMontante: number | null
  valorMoeda: string | null
  dataReferencia: string | null
  criadoEm: string
  atualizadoEm: string
}

export interface CriarAnotacaoRequest {
  titulo: string
  conteudo?: string
  tipo: TipoAnotacao
  prioridade: PrioridadeAnotacao
  valorMontante?: number
  valorMoeda?: string
  dataReferencia?: string
}

export type AtualizarAnotacaoRequest = CriarAnotacaoRequest
