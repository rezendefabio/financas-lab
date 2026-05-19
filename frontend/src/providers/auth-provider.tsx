'use client'
import { createContext, useContext, useState, useCallback } from 'react'
import { isAuthenticated, clearSession } from '@/shared/lib/auth'

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
    void clearSession()
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
