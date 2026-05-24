import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

let mockSidebar = { setOpen: vi.fn(), setOpenMobile: vi.fn(), isMobile: false }

vi.mock('@/shared/components/ui/sidebar', () => ({
  useSidebar: () => mockSidebar,
}))

import { useBreakpointSidebarCollapse } from './use-breakpoint-sidebar'

type MQL = {
  matches: boolean
  media: string
  addEventListener: ReturnType<typeof vi.fn>
  removeEventListener: ReturnType<typeof vi.fn>
  _listeners: Array<(e: { matches: boolean }) => void>
}

function makeMql(media: string, matches: boolean): MQL {
  const mql: MQL = {
    matches,
    media,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    _listeners: [],
  }
  mql.addEventListener.mockImplementation((_evt: string, cb: (e: { matches: boolean }) => void) => {
    mql._listeners.push(cb)
  })
  mql.removeEventListener.mockImplementation((_evt: string, cb: (e: { matches: boolean }) => void) => {
    mql._listeners = mql._listeners.filter((l) => l !== cb)
  })
  return mql
}

/**
 * Stub que retorna diferentes MQLs por media query.
 * `width` simula a largura do viewport.
 */
function stubMatchMedia(width: number) {
  const mqWide = makeMql('(min-width: 1280px)', width >= 1280)
  const mqTablet = makeMql('(min-width: 1024px)', width >= 1024)
  window.matchMedia = vi.fn((q: string) => {
    if (q.includes('1280')) return mqWide
    if (q.includes('1024')) return mqTablet
    throw new Error('unexpected media query: ' + q)
  }) as unknown as typeof window.matchMedia
  return { mqWide, mqTablet }
}

describe('useBreakpointSidebarCollapse', () => {
  beforeEach(() => {
    mockSidebar = { setOpen: vi.fn(), setOpenMobile: vi.fn(), isMobile: false }
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('viewport >= 1280px: nao chama setOpen na montagem (preserva estado do cookie)', () => {
    stubMatchMedia(1440)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).not.toHaveBeenCalled()
  })

  it('viewport 1024-1279px (tablet landscape): chama setOpen(false)', () => {
    stubMatchMedia(1100)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).toHaveBeenCalledWith(false)
  })

  it('viewport < 1024px (tablet portrait): chama setOpen(false)', () => {
    stubMatchMedia(900)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).toHaveBeenCalledWith(false)
  })

  it('isMobile=true: nao chama setOpen', () => {
    mockSidebar = { setOpen: vi.fn(), setOpenMobile: vi.fn(), isMobile: true }
    stubMatchMedia(1440)
    renderHook(() => useBreakpointSidebarCollapse())
    expect(mockSidebar.setOpen).not.toHaveBeenCalled()
  })

  it('registra e remove os listeners de mudanca nos dois breakpoints', () => {
    const { mqWide, mqTablet } = stubMatchMedia(1440)
    const { unmount } = renderHook(() => useBreakpointSidebarCollapse())
    expect(mqWide.addEventListener).toHaveBeenCalledWith('change', expect.any(Function))
    expect(mqTablet.addEventListener).toHaveBeenCalledWith('change', expect.any(Function))
    unmount()
    expect(mqWide.removeEventListener).toHaveBeenCalledWith('change', expect.any(Function))
    expect(mqTablet.removeEventListener).toHaveBeenCalledWith('change', expect.any(Function))
  })

  it('mudanca de < 1024px para >= 1280px: re-aplica e nao colapsa', () => {
    const { mqWide, mqTablet } = stubMatchMedia(900)
    renderHook(() => useBreakpointSidebarCollapse())
    // Montagem: < 1024px chamou setOpen(false)
    expect(mockSidebar.setOpen).toHaveBeenCalledWith(false)
    mockSidebar.setOpen.mockClear()

    // Simula expansao do viewport para >= 1280px
    act(() => {
      mqWide.matches = true
      mqTablet.matches = true
      mqWide._listeners.forEach((cb) => cb({ matches: true }))
    })

    // Faixa >= 1280px: nao deve chamar setOpen (respeita cookie)
    expect(mockSidebar.setOpen).not.toHaveBeenCalled()
  })
})
