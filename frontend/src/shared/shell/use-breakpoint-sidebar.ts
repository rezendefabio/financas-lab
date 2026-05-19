'use client'
import { useEffect } from 'react'
import { useSidebar } from '@/shared/components/ui/sidebar'

/**
 * Auto-colapsa/expande a sidebar conforme o breakpoint:
 * - < 1280px: colapsa para icon mode
 * - >= 1280px: expande
 *
 * Respeita mudancas manuais do usuario: o hook so age quando a janela
 * cruza o limiar de 1280px, nao em cada render.
 */
export function useBreakpointSidebarCollapse() {
  const { setOpen, isMobile } = useSidebar()

  useEffect(() => {
    if (isMobile) return // mobile usa Sheet, nao icon mode

    const mq = window.matchMedia('(min-width: 1280px)')

    const handler = (e: MediaQueryListEvent | MediaQueryList) => {
      setOpen(e.matches) // >= 1280px: abrir; < 1280px: colapsar para icon
    }

    // Colapsa automaticamente em telas pequenas na montagem.
    // Nao forca abertura em telas grandes -- respeita o estado persistido (cookie).
    if (!mq.matches) {
      setOpen(false)
    }
    mq.addEventListener('change', handler)
    return () => mq.removeEventListener('change', handler)
  }, [setOpen, isMobile])
}
