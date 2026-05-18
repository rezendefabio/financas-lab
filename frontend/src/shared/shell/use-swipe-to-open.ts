'use client'
import { useEffect } from 'react'
import { useSidebar } from '@/shared/components/ui/sidebar'

/**
 * Detecta swipe da borda esquerda (< 30px) para direita (> 80px)
 * e abre o sidebar no mobile.
 */
export function useSwipeToOpen() {
  const { isMobile, setOpenMobile } = useSidebar()

  useEffect(() => {
    if (!isMobile) return

    let startX = 0

    const onTouchStart = (e: TouchEvent) => {
      startX = e.touches[0].clientX
    }

    const onTouchEnd = (e: TouchEvent) => {
      const endX = e.changedTouches[0].clientX
      if (startX < 30 && endX - startX > 80) {
        setOpenMobile(true)
      }
    }

    document.addEventListener('touchstart', onTouchStart, { passive: true })
    document.addEventListener('touchend', onTouchEnd, { passive: true })
    return () => {
      document.removeEventListener('touchstart', onTouchStart)
      document.removeEventListener('touchend', onTouchEnd)
    }
  }, [isMobile, setOpenMobile])
}
