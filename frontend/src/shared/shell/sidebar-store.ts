/**
 * Store Zustand do estado de UI da sidebar.
 *
 * Guarda quais grupos do menu hierarquico estao colapsados (fechados).
 * Persistido em localStorage via middleware `persist` (ADR-014 decisao 2:
 * Zustand para estado de UI).
 *
 * Convencao: o conjunto `collapsed` armazena os nomes de grupo FECHADOS.
 * Um grupo ausente do conjunto esta aberto. Assim o estado default
 * (conjunto vazio) deixa todos os grupos abertos.
 */
'use client'

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

interface SidebarState {
  /** Chaves de grupo atualmente colapsadas (fechadas). */
  collapsed: string[]
  /** Alterna o estado aberto/fechado de um grupo. */
  toggleGroup: (groupKey: string) => void
  /** Indica se um grupo esta colapsado (fechado). */
  isCollapsed: (groupKey: string) => boolean
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      collapsed: [],
      toggleGroup: (groupKey) =>
        set((state) => {
          const isClosed = state.collapsed.includes(groupKey)
          return {
            collapsed: isClosed
              ? state.collapsed.filter((key) => key !== groupKey)
              : [...state.collapsed, groupKey],
          }
        }),
      isCollapsed: (groupKey) => get().collapsed.includes(groupKey),
    }),
    {
      name: 'financas-lab:sidebar',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)
