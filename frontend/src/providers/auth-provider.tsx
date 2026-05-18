'use client'
import { createContext, useContext, useState, useCallback } from 'react'
import { isAuthenticated, clearToken } from '@/shared/lib/auth'
import { useTabsStore } from '@/shared/shell/tabs-store'
import { useSidebarStore, initialCollapsed } from '@/shared/shell/sidebar-store'

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
    useTabsStore.setState({ tabs: [], activeId: null })
    useSidebarStore.setState({ collapsed: initialCollapsed })
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
