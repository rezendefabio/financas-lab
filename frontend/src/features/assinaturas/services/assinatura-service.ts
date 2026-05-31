import { apiFetch } from '@/services/api-client'
import type {
  Assinatura,
  CriarAssinaturaPayload,
  AtualizarAssinaturaPayload,
} from '../types/assinatura'

export const assinaturaService = {
  listar: () => apiFetch<Assinatura[]>('/api/assinaturas'),
  buscar: (id: string) => apiFetch<Assinatura>(`/api/assinaturas/${id}`),
  criar: (payload: CriarAssinaturaPayload) =>
    apiFetch<Assinatura>('/api/assinaturas', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  atualizar: (id: string, payload: AtualizarAssinaturaPayload) =>
    apiFetch<Assinatura>(`/api/assinaturas/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  remover: (id: string) =>
    apiFetch<void>(`/api/assinaturas/${id}`, { method: 'DELETE' }),
}
