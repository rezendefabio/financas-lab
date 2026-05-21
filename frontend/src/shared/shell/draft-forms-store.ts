'use client'

import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

interface DraftFormsState {
  /** Mapa de chave (pathname) -> valores serializados do formulario. */
  drafts: Record<string, unknown>
  save: (key: string, values: unknown) => void
  getDraft: (key: string) => unknown | null
  clear: (key: string) => void
}

export const useDraftFormsStore = create<DraftFormsState>()(
  persist(
    (set, get) => ({
      drafts: {},
      save: (key, values) =>
        set((state) => ({
          drafts: { ...state.drafts, [key]: values },
        })),
      getDraft: (key) => get().drafts[key] ?? null,
      clear: (key) =>
        set((state) => {
          const { [key]: _, ...rest } = state.drafts
          return { drafts: rest }
        }),
    }),
    {
      name: 'financas-lab:form-drafts',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)
