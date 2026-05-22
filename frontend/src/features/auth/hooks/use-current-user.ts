'use client'
import { useState, useEffect } from 'react'
import { useAuth } from '@/providers/auth-provider'
import { getCurrentUserEmail } from '@/shared/lib/auth'

export interface CurrentUser {
  email: string | null
  /** Inicial maiuscula do email para usar no avatar (ex: "f" de "fabio@..." -> "F"). */
  initials: string
}

export function useCurrentUser(): CurrentUser {
  const { loggedIn } = useAuth()
  // Comecar com null evita mismatch de hidratacao SSR: servidor e cliente
  // renderizam '?' inicialmente; apos mount o useEffect atualiza com o email real.
  const [email, setEmail] = useState<string | null>(null)

  useEffect(() => {
    setEmail(loggedIn ? getCurrentUserEmail() : null)
  }, [loggedIn])

  const initials = email ? email[0].toUpperCase() : '?'
  return { email, initials }
}
