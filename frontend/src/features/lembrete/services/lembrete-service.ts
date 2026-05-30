import { apiFetch } from '@/services/api-client'
import type {
  Lembrete,
  CriarLembretePayload,
  AtualizarLembretePayload,
} from '../types/lembrete'

const BASE_PATH = '/api/lembretes'

export async function listarLembretes(): Promise<Lembrete[]> {
  return apiFetch<Lembrete[]>(BASE_PATH)
}

export async function buscarLembrete(id: string): Promise<Lembrete> {
  return apiFetch<Lembrete>(`${BASE_PATH}/${id}`)
}

export async function criarLembrete(payload: CriarLembretePayload): Promise<Lembrete> {
  return apiFetch<Lembrete>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarLembrete(
  id: string,
  payload: AtualizarLembretePayload,
): Promise<Lembrete> {
  return apiFetch<Lembrete>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function excluirLembrete(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
