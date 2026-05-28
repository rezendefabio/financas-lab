import { apiFetch } from '@/services/api-client'
import type {
  FaturaResponse,
  CriarFaturaRequest,
  AtualizarFaturaRequest,
} from '../types/fatura'

const BASE_PATH = '/api/faturas'

export async function listarFaturas(): Promise<FaturaResponse[]> {
  return apiFetch<FaturaResponse[]>(BASE_PATH)
}

export async function buscarFatura(id: string): Promise<FaturaResponse> {
  return apiFetch<FaturaResponse>(`${BASE_PATH}/${id}`)
}

export async function criarFatura(
  payload: CriarFaturaRequest,
): Promise<FaturaResponse> {
  return apiFetch<FaturaResponse>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarFatura(
  id: string,
  payload: AtualizarFaturaRequest,
): Promise<FaturaResponse> {
  return apiFetch<FaturaResponse>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deletarFatura(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
