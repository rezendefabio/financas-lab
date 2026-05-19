import { getToken, clearToken } from '@/shared/lib/auth'
import { ApiError } from '@/shared/types/api'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

/**
 * Opcoes do apiFetch. Estende RequestInit com `screenCode` opcional, que
 * o backend usa para registrar a origem de uma mutacao na trilha de
 * auditoria (header `X-Screen-Code`).
 */
export type ApiFetchOptions = RequestInit & { screenCode?: string }

export async function apiFetch<T>(path: string, init?: ApiFetchOptions): Promise<T> {
  const token = getToken()
  const { screenCode, ...rest } = init ?? {}
  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(screenCode ? { 'X-Screen-Code': screenCode } : {}),
      ...(rest.headers ?? {}),
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

/**
 * Faz um GET autenticado e retorna o corpo da resposta como Blob.
 *
 * Usado para download de arquivos (CSV, PDF, etc.), onde o corpo nao e JSON.
 * Mantem o tratamento de 401 e de erro dos demais helpers.
 */
export async function apiFetchBlob(path: string): Promise<Blob> {
  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (res.status === 401) {
    clearToken()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sessao expirada. Faca login novamente.')
  }
  if (!res.ok) {
    throw new ApiError(res.status, res.statusText)
  }
  return res.blob()
}
