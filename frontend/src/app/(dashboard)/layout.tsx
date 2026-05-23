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
import { ErrorBoundary } from '@/shared/components/ErrorBoundary'

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
        <ErrorBoundary>
          <main className="flex-1 p-6">
            <Suspense fallback={null}>{children}</Suspense>
          </main>
        </ErrorBoundary>
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

  // Refs para distinguir troca de aba de navegacao dentro da aba.
  const prevActiveIdRef = useRef(activeId)
  const prevPathnameRef = useRef(pathname)
  // Sinaliza que o proximo pathname vem do TabBar (troca de aba), nao do usuario.
  const tabSwitchPendingRef = useRef(false)

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

  // Rastreia o path atual por aba e descarta rascunho quando o usuario navega
  // voluntariamente para fora do formulario dentro da mesma aba.
  //
  // O efeito pode disparar DUAS vezes numa troca de aba:
  //   1a passagem: activeId muda (sameTab=false) -> atualiza refs, sinaliza pendente
  //   2a passagem: pathname muda por router.replace do TabBar (sameTab=true agora) ->
  //     sem tabSwitchPendingRef o codigo identificaria errado como navegacao na aba
  //     e apagaria o rascunho. O flag evita isso.
  useEffect(() => {
    const sameTab = prevActiveIdRef.current === activeId
    const prevPathname = prevPathnameRef.current
    prevActiveIdRef.current = activeId
    prevPathnameRef.current = pathname

    if (!sameTab) {
      // Troca de aba: proximo pathname sera do TabBar, nao do usuario.
      tabSwitchPendingRef.current = true
      return
    }

    if (!activeId || prevPathname === pathname) return

    updateTabPath(activeId, pathname)

    if (tabSwitchPendingRef.current) {
      // Pathname mudou como efeito da troca de aba -- preserva rascunho.
      tabSwitchPendingRef.current = false
    } else {
      // Navegacao explicita dentro da aba (botao voltar, cancelar) -- descarta.
      clearDraft(prevPathname)
    }
  }, [pathname, activeId, updateTabPath, clearDraft])

  return (
    <SidebarProvider>
      <DashboardShell>{children}</DashboardShell>
    </SidebarProvider>
  )
}
