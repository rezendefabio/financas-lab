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

/**
 * Encerra a sessao completamente: remove o token e reseta os stores de UI.
 * Usar em logout explicito e em respostas 401 (token expirado).
 * Importacao dinamica dos stores evita dependencia circular.
 */
export async function clearSession(): Promise<void> {
  clearToken()
  try {
    // Importacao dinamica para evitar dependencia circular (auth.ts <- stores <- auth.ts)
    const [{ useTabsStore }, { useSidebarStore, initialCollapsed }] = await Promise.all([
      import('@/shared/shell/tabs-store'),
      import('@/shared/shell/sidebar-store'),
    ])
    useTabsStore.setState({ tabs: [], activeId: null })
    useSidebarStore.setState({ collapsed: initialCollapsed })
  } catch (err) {
    // O token ja foi removido; falha ao resetar os stores nao deve ser silenciosa.
    console.error('Falha ao resetar os stores de UI em clearSession:', err)
  }
}
