// Tipos da feature de notificacoes. Espelham o NotificacaoResponse do backend.

export type TipoNotificacao =
  | 'ORCAMENTO_ATENCAO'
  | 'ORCAMENTO_EXCEDIDO'
  | 'META_VENCENDO'
  | 'META_VENCIDA'

export interface Notificacao {
  id: string
  tipo: TipoNotificacao
  referenciaId: string
  titulo: string
  descricao: string
  criadoEm: string
}
