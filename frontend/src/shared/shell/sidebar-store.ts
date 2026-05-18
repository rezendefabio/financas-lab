/**
 * Store Zustand do estado de UI da sidebar.
 *
 * Guarda quais grupos do menu hierarquico estao colapsados (fechados).
 * Persistido em localStorage via middleware `persist` (ADR-014 decisao 2:
 * Zustand para estado de UI).
 *
 * Convencao: o conjunto `collapsed` armazena os nomes de grupo FECHADOS.
 * Estado default: todos os grupos fechados (getAllGroupKeys popula o array).
 */
'use client'

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import { buildMenuTree } from './menu-tree'
import { getAllScreens } from './screens.registry'
import type { MenuNode } from './menu-tree'

interface SidebarState {
  /** Chaves de grupo atualmente colapsadas (fechadas). */
  collapsed: string[]
  /** Alterna o estado aberto/fechado de um grupo. */
  toggleGroup: (groupKey: string) => void
  /** Indica se um grupo esta colapsado (fechado). */
  isCollapsed: (groupKey: string) => boolean
}

/** Coleta todas as chaves de grupos (nos nao-folha) da arvore de menu. */
function getAllGroupKeys(): string[] {
  const keys: string[] = []
  function collect(nodes: MenuNode[]) {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        keys.push(node.key)
        collect(node.children)
      }
    }
  }
  collect(buildMenuTree(getAllScreens()))
  return keys
}

const initialCollapsed = getAllGroupKeys()

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      collapsed: initialCollapsed,
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
      name: 'financas-lab:sidebar-v2',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)
