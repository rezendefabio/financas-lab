'use client'
/**
 * Re-export do hook de notificacoes, agora servido pelo backend
 * (feature `notificacoes`). Mantido neste path por compatibilidade com os
 * consumidores do shell (ShellHeader, useNotificacoesToast).
 *
 * Antes: as notificacoes eram CALCULADAS no frontend (orcamentos/metas) e o
 * descarte vivia so em estado local -- por isso reaparecia no proximo login.
 * Agora: materializadas e descartaveis no backend.
 */
export {
  useNotificacoes,
  useDescartarNotificacao,
} from '@/features/notificacoes/hooks/use-notificacoes'
export type {
  Notificacao,
  TipoNotificacao,
} from '@/features/notificacoes/types/notificacao'
