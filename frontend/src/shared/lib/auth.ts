const TOKEN_KEY = 'financas_token'

export const getToken = (): string | null =>
  typeof window !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null

export const setToken = (token: string): void =>
  localStorage.setItem(TOKEN_KEY, token)

export const clearToken = (): void =>
  localStorage.removeItem(TOKEN_KEY)

export const isAuthenticated = (): boolean =>
  !!getToken()

/** Decodifica o payload do JWT sem verificar assinatura. */
export function parseJwtPayload(token: string): Record<string, unknown> {
  try {
    const part = token.split('.')[1]
    return JSON.parse(atob(part.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return {}
  }
}

/** Retorna o email do usuario logado lendo o claim `sub` do JWT, ou null. */
export function getCurrentUserEmail(): string | null {
  const token = getToken()
  if (!token) return null
  const payload = parseJwtPayload(token)
  return typeof payload.sub === 'string' ? payload.sub : null
}
