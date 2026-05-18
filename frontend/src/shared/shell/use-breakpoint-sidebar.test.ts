import { renderHook } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

let mockSidebar = { setOpen: vi.fn(), isMobile: false }

vi.mock('@/shared/components/ui/sidebar', () => ({
  useSidebar: () => mockSidebar,
}))

import { useBreakpointSidebarCollapse } from './use-breakpoint-sidebar'

/** Instala um stub de window.matchMedia que reporta `matches`. */
function stubMatchMedia(matches: boolean) {
  const mql = {
    matches,
    media: '(min-width: 1280px)',
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }
  window.matchMedia = vi.fn().mockReturnValue(mql) as unknown as typeof window.matchMedia
  return mql
}

describe('useBreakpointSidebarCollapse', () => {
  beforeEach(() => {
    mockSidebar = { setOpen: vi.fn(), isMobile: false }
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('viewport >= 1280px: chama setOpen(true)', () => {
    stubMatchMedia(true)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).toHaveBeenCalledWith(true)
  })

  it('viewport < 1280px: chama setOpen(false)', () => {
    stubMatchMedia(false)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).toHaveBeenCalledWith(false)
  })

  it('isMobile=true: nao chama setOpen', () => {
    mockSidebar = { setOpen: vi.fn(), isMobile: true }
    stubMatchMedia(true)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).not.toHaveBeenCalled()
  })

  it('registra e remove o listener de mudanca de breakpoint', () => {
    const mql = stubMatchMedia(true)
    const { unmount } = renderHook(() => useBreakpointSidebarCollapse())
    expect(mql.addEventListener).toHaveBeenCalledWith('change', expect.any(Function))
    unmount()
    expect(mql.removeEventListener).toHaveBeenCalledWith('change', expect.any(Function))
  })
})
