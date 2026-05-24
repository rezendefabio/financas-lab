'use client'
import { useEffect } from 'react'
import { useSidebar } from '@/shared/components/ui/sidebar'

/**
 * Auto-colapsa/expande a sidebar conforme tres faixas de viewport (ADR-014):
 * - >= 1280px (desktop wide): respeita estado persistido (cookie); nao age
 * - 1024-1279px (tablet landscape / desktop estreito): colapsa para icon mode
 * - 768-1023px (tablet portrait): colapsa (oculta no desktop layout; shadcn
 *   nao oferece API para ocultar completamente em desktop, entao usa
 *   setOpen(false) -- icon mode permanece visivel como limitacao do componente)
 * - < 768px: nao age -- mobile usa Sheet via isMobile
 *
 * Respeita mudancas manuais do usuario: o hook so age quando a janela cruza
 * algum dos dois limiares, nao em cada render.
 */
export function useBreakpointSidebarCollapse() {
  const { setOpen, isMobile } = useSidebar()

  useEffect(() => {
    if (isMobile) return // mobile usa Sheet, nao icon mode

    const mqWide = window.matchMedia('(min-width: 1280px)')
    const mqTablet = window.matchMedia('(min-width: 1024px)')

    const apply = () => {
      // >= 1280px: respeita estado persistido (cookie); abaixo disso colapsa.
      // 768-1023px se comporta como 1024-1279px (ambos icon mode) por
      // limitacao do shadcn -- sem API para ocultar completamente em desktop.
      if (!mqWide.matches) {
        setOpen(false)
      }
    }

    apply()
    mqWide.addEventListener('change', apply)
    mqTablet.addEventListener('change', apply)
    return () => {
      mqWide.removeEventListener('change', apply)
      mqTablet.removeEventListener('change', apply)
    }
  }, [setOpen, isMobile])
}
