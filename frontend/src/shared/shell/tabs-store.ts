/**
 * Store Zustand do Tab Manager -- abas internas do app (ADR-014 fase 2,
 * spec secao 4.2).
 *
 * Cada aba referencia uma tela do Screen Registry pelo `screenCode`. A lista
 * de abas e a aba ativa sao persistidas em localStorage via middleware
 * `persist` (chave `financas-lab:tabs`). O TabBar sincroniza o estado com a
 * URL (`?tabs=...&active=...`); a URL tem prioridade sobre o localStorage.
 *
 * Limite: 10 abas. Ao atingir, a aba mais antiga nao-fixada e fechada para
 * abrir espaco. Se todas estiverem fixadas, a abertura e ignorada.
 */
'use client'

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

/** Aba aberta no Tab Manager. */
export interface Tab {
  /** uuid v4 gerado na criacao. */
  id: string
  /** Codigo da tela referenciada (ex: 'FIN-CTA-001'). */
  screenCode: string
  /** Aba fixada nao e fechada automaticamente pelo limite. */
  pinned: boolean
}

/** Numero maximo de abas abertas simultaneamente. */
export const MAX_TABS = 10

/** Codigo da tela Dashboard -- fallback quando a lista de abas esvazia. */
const DASHBOARD_CODE = 'REL-DSH-001'

/**
 * Gera um identificador unico. Usa `crypto.randomUUID` quando disponivel;
 * em ambientes sem (ex: jsdom antigo) cai para um id aleatorio simples.
 */
const genId = (): string =>
  typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : Math.random().toString(36).slice(2)

interface TabsState {
  /** Lista ordenada de abas abertas. */
  tabs: Tab[]
  /** Id da aba ativa, ou null quando nao ha abas. */
  activeId: string | null
  /**
   * Abre (ou ativa) a aba da tela informada.
   * - Se ja existe aba com esse `screenCode`: apenas ativa.
   * - Caso contrario: cria nova aba ao final e a ativa.
   * - Ao atingir o limite, fecha a aba mais antiga nao-fixada.
   * - Se o limite foi atingido e todas estao fixadas: ignora silenciosamente.
   */
  openTab: (screenCode: string) => void
  /**
   * Fecha a aba informada (aceita qualquer id, inclusive fixadas).
   * Se era a ativa, ativa a vizinha a esquerda; se nao houver, a da direita.
   * A lista nunca esvazia: ao fechar a ultima aba, o Dashboard e reaberto
   * automaticamente como aba ativa.
   */
  closeTab: (id: string) => void
  /** Define a aba ativa. */
  setActive: (id: string) => void
  /** Alterna o estado `pinned` da aba. */
  togglePin: (id: string) => void
  /** Move a aba da posicao `fromIndex` para `toIndex`. */
  reorder: (fromIndex: number, toIndex: number) => void
  /**
   * Duplica a aba: cria uma nova com o mesmo `screenCode` (novo id,
   * `pinned=false`) inserida logo apos a original, e a ativa.
   */
  duplicateTab: (id: string) => void
}

export const useTabsStore = create<TabsState>()(
  persist(
    (set) => ({
      tabs: [],
      activeId: null,

      openTab: (screenCode) =>
        set((state) => {
          const existing = state.tabs.find(
            (tab) => tab.screenCode === screenCode,
          )
          if (existing) {
            return { activeId: existing.id }
          }

          let tabs = state.tabs
          if (tabs.length >= MAX_TABS) {
            const oldestUnpinned = tabs.find((tab) => !tab.pinned)
            if (!oldestUnpinned) {
              // Limite atingido e todas as abas fixadas: ignora.
              return state
            }
            tabs = tabs.filter((tab) => tab.id !== oldestUnpinned.id)
          }

          const newTab: Tab = { id: genId(), screenCode, pinned: false }
          return { tabs: [...tabs, newTab], activeId: newTab.id }
        }),

      closeTab: (id) =>
        set((state) => {
          const index = state.tabs.findIndex((tab) => tab.id === id)
          if (index === -1) {
            return state
          }
          const tabs = state.tabs.filter((tab) => tab.id !== id)

          if (tabs.length === 0) {
            // Reabre o Dashboard automaticamente quando a lista esvazia.
            const dashTab: Tab = {
              id: genId(),
              screenCode: DASHBOARD_CODE,
              pinned: false,
            }
            return { tabs: [dashTab], activeId: dashTab.id }
          }

          let activeId = state.activeId
          if (state.activeId === id) {
            // Vizinha a esquerda; se nao houver, a da direita.
            const neighbor = tabs[index - 1] ?? tabs[index] ?? tabs[0]
            activeId = neighbor.id
          }
          return { tabs, activeId }
        }),

      setActive: (id) => set({ activeId: id }),

      togglePin: (id) =>
        set((state) => ({
          tabs: state.tabs.map((tab) =>
            tab.id === id ? { ...tab, pinned: !tab.pinned } : tab,
          ),
        })),

      reorder: (fromIndex, toIndex) =>
        set((state) => {
          if (
            fromIndex < 0 ||
            fromIndex >= state.tabs.length ||
            toIndex < 0 ||
            toIndex >= state.tabs.length ||
            fromIndex === toIndex
          ) {
            return state
          }
          const tabs = [...state.tabs]
          const [moved] = tabs.splice(fromIndex, 1)
          tabs.splice(toIndex, 0, moved)
          return { tabs }
        }),

      duplicateTab: (id) =>
        set((state) => {
          const index = state.tabs.findIndex((tab) => tab.id === id)
          if (index === -1) {
            return state
          }
          const original = state.tabs[index]
          const copy: Tab = {
            id: genId(),
            screenCode: original.screenCode,
            pinned: false,
          }
          const tabs = [...state.tabs]
          tabs.splice(index + 1, 0, copy)
          return { tabs, activeId: copy.id }
        }),
    }),
    {
      name: 'financas-lab:tabs',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ tabs: state.tabs, activeId: state.activeId }),
    },
  ),
)

/** Codigo da tela Dashboard, exposto para consumo externo (fallback). */
export { DASHBOARD_CODE }
