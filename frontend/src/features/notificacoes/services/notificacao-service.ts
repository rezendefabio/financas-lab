import { apiFetch } from '@/services/api-client'
import type { Notificacao } from '../types/notificacao'

export const notificacoesService = {
  listar: (): Promise<Notificacao[]> => apiFetch<Notificacao[]>('/api/notificacoes'),

  descartar: (id: string): Promise<void> =>
    apiFetch<void>(`/api/notificacoes/${id}/descartar`, { method: 'PATCH' }),
}
