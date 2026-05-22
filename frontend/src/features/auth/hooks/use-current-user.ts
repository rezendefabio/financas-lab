'use client'
import { useSyncExternalStore } from 'react'
import { useAuth } from '@/providers/auth-provider'
import { getCurrentUserEmail } from '@/shared/lib/auth'

export interface CurrentUser {
  email: string | null
  /** Inicial maiuscula do email para usar no avatar (ex: "f" de "fabio@..." -> "F"). */
  initials: string
}

// useSyncExternalStore: servidor retorna null (evita hydration mismatch);
// cliente le o email do localStorage. useAuth() subscreve ao contexto para
// re-renderizar em login/logout.
function subscribe(cb: () => void) {
  window.addEventListener('storage', cb)
  return () => window.removeEventListener('storage', cb)
}

function getSnapshot(): string | null {
  return getCurrentUserEmail()
}

function getServerSnapshot(): null {
  return null
}

export function useCurrentUser(): CurrentUser {
  useAuth()
  const email = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
  const initials = email ? email[0].toUpperCase() : '?'
  return { email, initials }
}
