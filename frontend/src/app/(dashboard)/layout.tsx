'use client'
import { Suspense, useEffect, useRef } from 'react'
import { useRouter, usePathname, useSearchParams } from 'next/navigation'
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
import { ErrorBoundary } from '@/shared/components/ErrorBoundary'
import { ErrorBanner } from '@/shared/shell/ErrorBanner'
import { useGlobalErrorHandler } from '@/shared/hooks/useGlobalErrorHandler'
import { useNotificacoesToast } from '@/shared/hooks/useNotificacoesToast'

/**
 * Conteudo do shell. Componente separado para que os hooks de responsividade
 * (`useBreakpointSidebarCollapse`, `useSwipeToOpen`) possam consumir o
 * `useSidebar()` -- que exige estar dentro do `SidebarProvider`.
 */
function DashboardShell({ children }: { children: React.ReactNode }) {
  useBreakpointSidebarCollapse()
  useSwipeToOpen()
  useGlobalErrorHandler()
  useNotificacoesToast()

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
        <ErrorBanner />
        {/* TabBar nao le mais a URL (estado das abas vive no localStorage):
            nao precisa de fronteira Suspense. */}
        <TabBar />
        {/* Paginas de listagem (useListPage) tambem leem useSearchParams:
            isolar o conteudo numa fronteira Suspense evita o CSR bailout
            no prerender (Next.js missing-suspense-with-csr-bailout). */}
        <ErrorBoundary>
          <main className="flex-1 p-4 md:p-6 max-w-screen-2xl mx-auto w-full">
            <Suspense fallback={null}>{children}</Suspense>
          </main>
        </ErrorBoundary>
      </SidebarInset>
      <CommandPalette />
    </>
  )
}

/**
 * Rastreia o path completo (pathname + search params) da aba ativa e
 * persiste em useTabsStore via updateTabPath.
 *
 * Componente separado porque useSearchParams() exige fronteira Suspense
 * no Next.js App Router quando usado em layouts.
 */
export function TabPathTracker() {
  const pathname = usePathname()
  const searchParams = useSearchParams()
  // Filtrar params de controle do TabBar (tabs, active) -- eles sao gerenciados
  // pelo TabBar separadamente e nao devem fazer parte do "path da pagina".
  // Salvar esses params em currentPath causaria loop: TabBar leria o currentPath
  // com ?tabs=...&active=..., concatenaria novo ?tabs=...&active=... e a URL
  // cresceria infinitamente a cada render.
  const fullPath = (() => {
    const filtered = new URLSearchParams(searchParams.toString())
    filtered.delete('tabs')
    filtered.delete('active')
    const qs = filtered.toString()
    return qs ? `${pathname}?${qs}` : pathname
  })()

  const activeId = useTabsStore((s) => s.activeId)
  const updateTabPath = useTabsStore((s) => s.updateTabPath)
  const clearDraft = useDraftFormsStore((s) => s.clear)

  const prevActiveIdRef = useRef(activeId)
  const prevFullPathRef = useRef(fullPath)
  const tabSwitchPendingRef = useRef(false)

  useEffect(() => {
    const sameTab = prevActiveIdRef.current === activeId
    const prevPath = prevFullPathRef.current
    prevActiveIdRef.current = activeId
    prevFullPathRef.current = fullPath

    if (!sameTab) {
      tabSwitchPendingRef.current = true
      return
    }

    if (!activeId || prevPath === fullPath) return

    updateTabPath(activeId, fullPath)

    if (tabSwitchPendingRef.current) {
      tabSwitchPendingRef.current = false
    } else {
      // Navegacao explicita na aba (nao troca de aba) -- descarta rascunho do path anterior
      clearDraft(prevPath)
    }
  }, [fullPath, activeId, updateTabPath, clearDraft])

  return null
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const auth = useAuth()

  const openTab = useTabsStore((state) => state.openTab)

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

  return (
    <SidebarProvider>
      <Suspense fallback={null}>
        <TabPathTracker />
      </Suspense>
      <DashboardShell>{children}</DashboardShell>
    </SidebarProvider>
  )
}
