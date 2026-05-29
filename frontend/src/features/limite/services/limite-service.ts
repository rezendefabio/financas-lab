import { apiFetch } from '@/services/api-client'
import type {
  Limite,
  CriarLimitePayload,
  AtualizarLimitePayload,
} from '../types/limite'

const BASE_PATH = '/api/limites'

export async function listarLimites(): Promise<Limite[]> {
  return apiFetch<Limite[]>(BASE_PATH)
}

export async function buscarLimite(id: string): Promise<Limite> {
  return apiFetch<Limite>(`${BASE_PATH}/${id}`)
}

export async function criarLimite(payload: CriarLimitePayload): Promise<Limite> {
  return apiFetch<Limite>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarLimite(
  id: string,
  payload: AtualizarLimitePayload,
): Promise<Limite> {
  return apiFetch<Limite>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function desativarLimite(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
