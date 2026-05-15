import { apiFetch } from '@/services/api-client'
import type { Tag, CriarTagRequest } from '../types/tag'

export async function listarTags(): Promise<Tag[]> {
  return apiFetch<Tag[]>('/api/tags')
}

export async function criarTag(data: CriarTagRequest): Promise<Tag> {
  return apiFetch<Tag>('/api/tags', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function atualizarTag(id: string, data: Partial<CriarTagRequest>): Promise<Tag> {
  return apiFetch<Tag>(`/api/tags/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deletarTag(id: string): Promise<void> {
  return apiFetch<void>(`/api/tags/${id}`, {
    method: 'DELETE',
  })
}
