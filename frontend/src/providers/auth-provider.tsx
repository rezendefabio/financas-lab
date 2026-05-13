'use client'
import { createContext, useContext, useState, useCallback } from 'react'
import { isAuthenticated, clearToken } from '@/shared/lib/auth'

interface AuthContextValue {
  loggedIn: boolean
  logout: () => void
  refresh: () => void
}

const AuthContext = createContext<AuthContextValue>({
  loggedIn: false,
  logout: () => {},
  refresh: () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [loggedIn, setLoggedIn] = useState(() => isAuthenticated())

  const logout = useCallback(() => {
    clearToken()
    setLoggedIn(false)
  }, [])

  const refresh = useCallback(() => {
    setLoggedIn(isAuthenticated())
  }, [])

  return (
    <AuthContext.Provider value={{ loggedIn, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
