/**
 * Store Zustand do estado de abertura do CommandPalette.
 *
 * Extraido do `useState` local do `CommandPalette` (UI-2) para que outros
 * componentes -- notadamente o botao "+" do TabBar -- possam abrir o palette
 * sem prop drilling. Estado efemero: NAO persiste em localStorage.
 */
'use client'

import { create } from 'zustand'

interface CommandPaletteState {
  /** Indica se o palette esta aberto. */
  open: boolean
  /** Define o estado de abertura do palette. */
  setOpen: (open: boolean) => void
}

export const useCommandPaletteStore = create<CommandPaletteState>((set) => ({
  open: false,
  setOpen: (open) => set({ open }),
}))
