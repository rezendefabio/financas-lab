import '@testing-library/jest-dom'
import { vi, beforeEach } from 'vitest'
import { useDraftFormsStore } from '@/shared/shell/draft-forms-store'

// Limpa localStorage e reseta stores Zustand entre testes para evitar
// contaminacao entre testes que usam useDraftForm.
beforeEach(() => {
  localStorage.clear()
  useDraftFormsStore.setState({ drafts: {} })
})

// Polyfills de ambiente jsdom -- APIs de browser que o jsdom nao implementa
// e que componentes do shell (Sidebar, cmdk) consomem.

if (!window.matchMedia) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
}

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = vi.fn()
}
