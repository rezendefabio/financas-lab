import { apiFetch } from '@/services/api-client'
import type {
  Grupo,
  CriarGrupoPayload,
  AtualizarGrupoPayload,
} from '../types/grupo'

const BASE_PATH = '/api/grupos'

export async function listarGrupos(): Promise<Grupo[]> {
  return apiFetch<Grupo[]>(BASE_PATH)
}

export async function criarGrupo(payload: CriarGrupoPayload): Promise<Grupo> {
  return apiFetch<Grupo>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarGrupo(
  id: string,
  payload: AtualizarGrupoPayload,
): Promise<Grupo> {
  return apiFetch<Grupo>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deletarGrupo(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
