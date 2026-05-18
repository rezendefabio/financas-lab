/**
 * ShellHeader -- barra superior do shell (ADR-014 fase UI-3, spec secao 4.4).
 *
 * Mostra o toggle da sidebar, o titulo da tela ativa e, no mobile, um badge
 * com a contagem de abas abertas. A classe `shell-header` no `<header>` e
 * consumida pelos print styles em `globals.css` para esconder o shell na
 * impressao.
 */
'use client'
import { usePathname } from 'next/navigation'
import { Separator } from '@/shared/components/ui/separator'
import { SidebarTrigger, useSidebar } from '@/shared/components/ui/sidebar'
import { findScreenByPath } from './screens.registry'
import { useTabsStore } from './tabs-store'

export function ShellHeader() {
  const pathname = usePathname()
  const { isMobile } = useSidebar()
  const tabCount = useTabsStore((state) => state.tabs.length)
  const screen = findScreenByPath(pathname)

  return (
    <header className="flex h-12 shrink-0 items-center gap-2 border-b border-border px-4 shell-header">
      <SidebarTrigger className="-ml-1" />
      <Separator orientation="vertical" className="mr-2 h-4" />
      <span className="text-sm font-medium truncate">
        {screen?.title ?? 'Financas Lab'}
      </span>
      {isMobile && tabCount > 1 && (
        <span className="ml-auto rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
          {tabCount} abas
        </span>
      )}
    </header>
  )
}
