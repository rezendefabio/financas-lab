import { getToken, clearToken } from '@/shared/lib/auth'
import { ApiError } from '@/shared/types/api'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })
  if (res.status === 204) return undefined as T
  if (res.status === 401) {
    clearToken()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sessao expirada. Faca login novamente.')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    if (res.status === 500 && body.codigoErro) {
      throw new ApiError(
        500,
        `Erro inesperado (${body.codigoErro}). Informe ao suporte.`,
      )
    }
    throw new ApiError(res.status, body.message ?? res.statusText)
  }
  return res.json() as Promise<T>
}

/**
 * Envia um corpo multipart/form-data ao backend.
 *
 * Nao define o header Content-Type: o browser o gera automaticamente com o
 * boundary correto a partir do FormData. Mantem o tratamento de erro e o
 * cabecalho de autenticacao do apiFetch.
 */
export async function apiFetchMultipart<T>(
  path: string,
  formData: FormData,
): Promise<T> {
  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  })
  if (res.status === 204) return undefined as T
  if (res.status === 401) {
    clearToken()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sessao expirada. Faca login novamente.')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    if (res.status === 500 && body.codigoErro) {
      throw new ApiError(
        500,
        `Erro inesperado (${body.codigoErro}). Informe ao suporte.`,
      )
    }
    throw new ApiError(res.status, body.detail ?? body.message ?? res.statusText)
  }
  return res.json() as Promise<T>
}
