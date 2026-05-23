/**
 * Store Zustand do banner global de erros assincronos (sub-etapa UI-14,
 * Fase 2 da estrategia de erros). Captura erros que nao quebram o React
 * (promises rejeitadas, erros em event handlers) e os exibe entre o
 * ShellHeader e o TabBar, sem interromper o uso da aplicacao.
 *
 * Nao persiste em localStorage -- banners sao efemeros, descartados ao
 * recarregar a pagina ou ao serem fechados pelo usuario. Limite de 3
 * banners simultaneos: o mais antigo e descartado quando um quarto e
 * adicionado.
 */
'use client'

import { create } from 'zustand'

export interface ErrorBannerItem {
  /** Codigo ERR-XXXXXXXX retornado pelo backend, ou null enquanto registra. */
  codigo: string | null
  /** Mensagem tecnica simplificada para o usuario. */
  mensagem: string
  /** Tipo de erro (ex: TypeError, ReferenceError). */
  tipo: string
  /** Timestamp ISO de quando o erro ocorreu. */
  criadoEm: string
  /** ID interno para dismiss individual. */
  id: string
}

interface ErrorBannerState {
  banners: ErrorBannerItem[]
  /** Adiciona um banner. Limita a 3 simultaneos (descarta o mais antigo). */
  addBanner: (item: Omit<ErrorBannerItem, 'id'>) => void
  /** Atualiza o codigo de um banner (chamado apos o backend responder). */
  updateCodigo: (id: string, codigo: string) => void
  dismiss: (id: string) => void
  dismissAll: () => void
}

const MAX_BANNERS = 3

export const useErrorBannerStore = create<ErrorBannerState>()((set) => ({
  banners: [],

  addBanner: (item) =>
    set((state) => {
      const id =
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : Math.random().toString(36).slice(2)
      const banners = [...state.banners, { ...item, id }]
      return { banners: banners.slice(-MAX_BANNERS) }
    }),

  updateCodigo: (id, codigo) =>
    set((state) => ({
      banners: state.banners.map((b) =>
        b.id === id ? { ...b, codigo } : b,
      ),
    })),

  dismiss: (id) =>
    set((state) => ({
      banners: state.banners.filter((b) => b.id !== id),
    })),

  dismissAll: () => set({ banners: [] }),
}))
