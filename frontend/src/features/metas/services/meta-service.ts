import { apiFetch } from '@/services/api-client'
import type { Meta, CriarMetaPayload, RegistrarDepositoPayload } from '../types/meta'

export const metaService = {
  listar: () => apiFetch<Meta[]>('/api/metas'),
  buscar: (id: string) => apiFetch<Meta>(`/api/metas/${id}`),
  criar: (payload: CriarMetaPayload) =>
    apiFetch<Meta>('/api/metas', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  cancelar: (id: string) =>
    apiFetch<void>(`/api/metas/${id}`, { method: 'DELETE' }),
  registrarDeposito: (id: string, payload: RegistrarDepositoPayload) =>
    apiFetch<Meta>(`/api/metas/${id}/depositos`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
}
