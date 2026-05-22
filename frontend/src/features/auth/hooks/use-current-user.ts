'use client'
import { useAuth } from '@/providers/auth-provider'
import { getCurrentUserEmail } from '@/shared/lib/auth'

export interface CurrentUser {
  email: string | null
  /** Inicial maiuscula do email para usar no avatar (ex: "f" de "fabio@..." -> "F"). */
  initials: string
}

export function useCurrentUser(): CurrentUser {
  // useAuth() subscreve ao contexto de autenticacao: quando loggedIn muda
  // (login ou logout), este hook re-renderiza e re-le o email do localStorage.
  useAuth()
  const email = typeof window !== 'undefined' ? getCurrentUserEmail() : null
  const initials = email ? email[0].toUpperCase() : '?'
  return { email, initials }
}
