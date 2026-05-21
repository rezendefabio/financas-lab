import { apiFetch } from '@/services/api-client'
import type {
  CentroCusto,
  CriarCentroCustoPayload,
  AtualizarCentroCustoPayload,
} from '../types/centrocusto'

const BASE_PATH = '/api/centros-custo'

export async function listarCentrosCusto(): Promise<CentroCusto[]> {
  return apiFetch<CentroCusto[]>(BASE_PATH)
}

export async function buscarCentroCusto(id: string): Promise<CentroCusto> {
  return apiFetch<CentroCusto>(`${BASE_PATH}/${id}`)
}

export async function criarCentroCusto(
  payload: CriarCentroCustoPayload,
): Promise<CentroCusto> {
  return apiFetch<CentroCusto>(BASE_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function atualizarCentroCusto(
  id: string,
  payload: AtualizarCentroCustoPayload,
): Promise<CentroCusto> {
  return apiFetch<CentroCusto>(`${BASE_PATH}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function desativarCentroCusto(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, {
    method: 'DELETE',
  })
}
