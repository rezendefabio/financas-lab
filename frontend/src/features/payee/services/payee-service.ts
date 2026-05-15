import { apiFetch } from '@/services/api-client'
import type { Payee, CriarPayeeRequest } from '../types/payee'

export async function listarPayees(): Promise<Payee[]> {
  return apiFetch<Payee[]>('/api/payees')
}

export async function criarPayee(data: CriarPayeeRequest): Promise<Payee> {
  return apiFetch<Payee>('/api/payees', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function atualizarPayee(id: string, data: Partial<CriarPayeeRequest>): Promise<Payee> {
  return apiFetch<Payee>(`/api/payees/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deletarPayee(id: string): Promise<void> {
  return apiFetch<void>(`/api/payees/${id}`, {
    method: 'DELETE',
  })
}
