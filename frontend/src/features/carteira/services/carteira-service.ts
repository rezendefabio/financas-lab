import { apiFetch } from '@/services/api-client'
import type {
  CarteiraResponse,
  CriarCarteiraRequest,
  AtualizarCarteiraRequest,
} from '../types/carteira'

const BASE_PATH = '/api/carteiras'

export async function listarCarteiras(): Promise<CarteiraResponse[]> {
  return apiFetch<CarteiraResponse[]>(BASE_PATH)
}

export async function buscarCarteira(id: string): Promise<CarteiraResponse> {
  return apiFetch<CarteiraResponse>(`${BASE_PATH}/${id}`)
}

export async function criarCarteira(
  payload: CriarCarteiraRequest,
): Promise<CarteiraResponse> {
  return apiFetch<CarteiraResponse>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarCarteira(
  id: string,
  payload: AtualizarCarteiraRequest,
): Promise<CarteiraResponse> {
  return apiFetch<CarteiraResponse>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deletarCarteira(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
