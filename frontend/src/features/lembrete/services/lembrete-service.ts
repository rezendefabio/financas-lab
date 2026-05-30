import { apiFetch } from '@/services/api-client'
import type {
  LembreteResponse,
  CriarLembretePayload,
  AtualizarLembretePayload,
} from '../types/lembrete'

export const lembreteService = {
  listar: (): Promise<LembreteResponse[]> =>
    apiFetch<LembreteResponse[]>('/api/lembretes'),
  buscarPorId: (id: string): Promise<LembreteResponse> =>
    apiFetch<LembreteResponse>(`/api/lembretes/${id}`),
  criar: (payload: CriarLembretePayload): Promise<LembreteResponse> =>
    apiFetch<LembreteResponse>('/api/lembretes', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  atualizar: (
    id: string,
    payload: AtualizarLembretePayload,
  ): Promise<LembreteResponse> =>
    apiFetch<LembreteResponse>(`/api/lembretes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  deletar: (id: string): Promise<void> =>
    apiFetch<void>(`/api/lembretes/${id}`, { method: 'DELETE' }),
}
