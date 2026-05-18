import { renderHook } from '@testing-library/react'
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'

let mockSidebar = { setOpenMobile: vi.fn(), isMobile: true }

vi.mock('@/shared/components/ui/sidebar', () => ({
  useSidebar: () => mockSidebar,
}))

import { useSwipeToOpen } from './use-swipe-to-open'

/** Captura os handlers registrados em document.addEventListener por tipo de evento. */
let handlers: Record<string, (e: unknown) => void>

function touchStart(clientX: number) {
  handlers.touchstart?.({ touches: [{ clientX }] })
}

function touchEnd(clientX: number) {
  handlers.touchend?.({ changedTouches: [{ clientX }] })
}

describe('useSwipeToOpen', () => {
  beforeEach(() => {
    mockSidebar = { setOpenMobile: vi.fn(), isMobile: true }
    handlers = {}
    vi.spyOn(document, 'addEventListener').mockImplementation(((type: string, handler: unknown) => {
      handlers[type] = handler as (e: unknown) => void
    }) as typeof document.addEventListener)
    vi.spyOn(document, 'removeEventListener')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('swipe da borda esquerda para a direita: chama setOpenMobile(true)', () => {
    renderHook(() => useSwipeToOpen())
    touchStart(10) // borda esquerda (< 30px)
    touchEnd(120) // deslocamento de 110px (> 80px)
    expect(mockSidebar.setOpenMobile).toHaveBeenCalledWith(true)
  })

  it('deslocamento insuficiente (<= 80px): nao chama setOpenMobile', () => {
    renderHook(() => useSwipeToOpen())
    touchStart(10)
    touchEnd(70) // deslocamento de 60px
    expect(mockSidebar.setOpenMobile).not.toHaveBeenCalled()
  })

  it('toque iniciado fora da borda (>= 30px): nao chama setOpenMobile', () => {
    renderHook(() => useSwipeToOpen())
    touchStart(100) // longe da borda esquerda
    touchEnd(220) // deslocamento de 120px
    expect(mockSidebar.setOpenMobile).not.toHaveBeenCalled()
  })

  it('isMobile=false: nao registra listeners nem chama setOpenMobile', () => {
    mockSidebar = { setOpenMobile: vi.fn(), isMobile: false }
    renderHook(() => useSwipeToOpen())
    expect(document.addEventListener).not.toHaveBeenCalled()
    expect(handlers.touchstart).toBeUndefined()
    expect(mockSidebar.setOpenMobile).not.toHaveBeenCalled()
  })

  it('registra e remove os listeners de touch', () => {
    const { unmount } = renderHook(() => useSwipeToOpen())
    expect(document.addEventListener).toHaveBeenCalledWith(
      'touchstart',
      expect.any(Function),
      { passive: true },
    )
    expect(document.addEventListener).toHaveBeenCalledWith(
      'touchend',
      expect.any(Function),
      { passive: true },
    )
    unmount()
    expect(document.removeEventListener).toHaveBeenCalledWith('touchstart', expect.any(Function))
    expect(document.removeEventListener).toHaveBeenCalledWith('touchend', expect.any(Function))
  })
})
