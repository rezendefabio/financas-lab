'use client'
import { useSyncExternalStore } from 'react'
import { getCurrentUserEmail } from '@/shared/lib/auth'

export interface CurrentUser {
  email: string | null
  /** Inicial maiuscula do email para usar no avatar (ex: "f" de "fabio@..." -> "F"). */
  initials: string
}

// SSR retorna null; cliente le o localStorage apos hidratacao.
// useSyncExternalStore suprime o aviso de mismatch quando os snapshots diferem.
const subscribe = () => () => {}
const getSnapshot = () => getCurrentUserEmail()
const getServerSnapshot = (): null => null

export function useCurrentUser(): CurrentUser {
  const email = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
  const initials = email ? email[0].toUpperCase() : '?'
  return { email, initials }
}
