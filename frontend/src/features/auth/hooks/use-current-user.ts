'use client'
import { getCurrentUserEmail } from '@/shared/lib/auth'

export interface CurrentUser {
  email: string | null
  /** Inicial maiuscula do email para usar no avatar (ex: "f" de "fabio@..." -> "F"). */
  initials: string
}

export function useCurrentUser(): CurrentUser {
  const email = getCurrentUserEmail()
  const initials = email ? email[0].toUpperCase() : '?'
  return { email, initials }
}
