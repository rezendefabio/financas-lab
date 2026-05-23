import { getToken, clearSession } from '@/shared/lib/auth'
import { ApiError } from '@/shared/types/api'
import { useErrorBannerStore } from '@/shared/shell/error-banner-store'

/**
 * Publica um banner global de erro 500 com o codigo retornado pelo
 * backend. Mantemos a publicacao centralizada aqui (em vez de cada
 * service) porque o tratamento de respostas HTTP >= 500 ja e unico
 * neste arquivo (UI-14, Fase 2).
 */
function publishServerErrorBanner(body: {
  codigoErro?: string
  detail?: string
  message?: string
}): void {
  if (typeof window === 'undefined') return
  useErrorBannerStore.getState().addBanner({
    codigo: body.codigoErro ?? null,
    mensagem: body.detail ?? body.message ?? 'Erro interno do servidor',
    tipo: 'ServerError',
    criadoEm: new Date().toISOString(),
  })
}

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
    void clearSession()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sessao expirada. Faca login novamente.')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    if (res.status >= 500) {
      publishServerErrorBanner(body)
      if (body.codigoErro) {
        throw new ApiError(
          res.status,
          `Erro inesperado (${body.codigoErro}). Informe ao suporte.`,
        )
      }
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
    void clearSession()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new ApiError(401, 'Sessao expirada. Faca login novamente.')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    if (res.status >= 500) {
      publishServerErrorBanner(body)
      if (body.codigoErro) {
        throw new ApiError(
          res.status,
          `Erro inesperado (${body.codigoErro}). Informe ao suporte.`,
        )
      }
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
    void clearSession()
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
