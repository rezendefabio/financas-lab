'use client'
import { Suspense, useEffect, useRef } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuth } from '@/features/auth/hooks/use-auth'
import {
  SidebarProvider,
  Sidebar,
  SidebarContent,
  SidebarInset,
  SidebarRail,
} from '@/shared/components/ui/sidebar'
import {
  SidebarNav,
  CommandPalette,
  TabBar,
  ShellHeader,
  useTabsStore,
  findScreenByPath,
  useBreakpointSidebarCollapse,
  useSwipeToOpen,
} from '@/shared/shell'
import { useDraftFormsStore } from '@/shared/shell/draft-forms-store'

/**
 * Conteudo do shell. Componente separado para que os hooks de responsividade
 * (`useBreakpointSidebarCollapse`, `useSwipeToOpen`) possam consumir o
 * `useSidebar()` -- que exige estar dentro do `SidebarProvider`.
 */
function DashboardShell({ children }: { children: React.ReactNode }) {
  useBreakpointSidebarCollapse()
  useSwipeToOpen()

  return (
    <>
      <Sidebar collapsible="icon">
        <SidebarContent>
          <SidebarNav />
        </SidebarContent>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <ShellHeader />
        {/* TabBar usa useSearchParams: precisa de fronteira Suspense para
            nao forcar CSR bailout no prerender das paginas filhas. */}
        <Suspense fallback={null}>
          <TabBar />
        </Suspense>
        {/* Paginas de listagem (useListPage) tambem leem useSearchParams:
            isolar o conteudo numa fronteira Suspense evita o CSR bailout
            no prerender (Next.js missing-suspense-with-csr-bailout). */}
        <main className="flex-1 p-6">
          <Suspense fallback={null}>{children}</Suspense>
        </main>
      </SidebarInset>
      <CommandPalette />
    </>
  )
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const auth = useAuth()

  const openTab = useTabsStore((state) => state.openTab)
  const activeId = useTabsStore((state) => state.activeId)
  const updateTabPath = useTabsStore((state) => state.updateTabPath)
  const clearDraft = useDraftFormsStore((state) => state.clear)

  // Ref para detectar troca de aba: so atualiza currentPath quando o usuario
  // navega dentro da aba ativa, nao quando o TabBar troca de aba.
  const prevActiveIdRef = useRef(activeId)
  const prevPathnameRef = useRef(pathname)

  useEffect(() => {
    if (!auth.loggedIn) {
      router.push('/login')
    }
  }, [auth.loggedIn, router])

  // Primeiro acesso (sem abas): abre a aba da tela atual.
  // Navegacao reativa e feita pelo TabBar (URL sync com targetPath da screen ativa).
  useEffect(() => {
    if (useTabsStore.getState().tabs.length === 0) {
      const screen = findScreenByPath(pathname)
      if (screen) openTab(screen.code)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Rastreia o path atual por aba: quando pathname muda SEM troca de aba
  // (usuario navegou dentro da aba), salva o novo path na aba ativa e
  // descarta o rascunho da pagina anterior (usuario saiu voluntariamente).
  // Quando ha troca de aba (sameTab=false), o rascunho e preservado.
  useEffect(() => {
    const sameTab = prevActiveIdRef.current === activeId
    const prevPathname = prevPathnameRef.current
    prevActiveIdRef.current = activeId
    prevPathnameRef.current = pathname
    if (sameTab && activeId) {
      updateTabPath(activeId, pathname)
      if (prevPathname !== pathname) {
        // Navegacao dentro da aba: descarta rascunho da pagina abandonada.
        clearDraft(prevPathname)
      }
    }
  }, [pathname, activeId, updateTabPath, clearDraft])

  return (
    <SidebarProvider>
      <DashboardShell>{children}</DashboardShell>
    </SidebarProvider>
  )
}
