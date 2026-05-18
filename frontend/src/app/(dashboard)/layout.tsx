'use client'
import { Suspense, useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuth } from '@/features/auth/hooks/use-auth'
import {
  SidebarProvider,
  Sidebar,
  SidebarContent,
  SidebarHeader,
  SidebarFooter,
  SidebarInset,
  SidebarRail,
} from '@/shared/components/ui/sidebar'
import { LogOut } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
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

/**
 * Conteudo do shell. Componente separado para que os hooks de responsividade
 * (`useBreakpointSidebarCollapse`, `useSwipeToOpen`) possam consumir o
 * `useSidebar()` -- que exige estar dentro do `SidebarProvider`.
 */
function DashboardShell({ children }: { children: React.ReactNode }) {
  const auth = useAuth()

  useBreakpointSidebarCollapse()
  useSwipeToOpen()

  return (
    <>
      <Sidebar collapsible="icon">
        <SidebarHeader className="px-4 py-3">
          <span className="font-semibold text-lg">Financas Lab</span>
        </SidebarHeader>
        <SidebarContent>
          <SidebarNav />
        </SidebarContent>
        <SidebarFooter className="px-4 py-3">
          <Button
            variant="ghost"
            className="w-full justify-start gap-2"
            onClick={auth.logout}
          >
            <LogOut className="h-4 w-4" />
            Sair
          </Button>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <ShellHeader />
        {/* TabBar usa useSearchParams: precisa de fronteira Suspense para
            nao forcar CSR bailout no prerender das paginas filhas. */}
        <Suspense fallback={null}>
          <TabBar />
        </Suspense>
        <main className="flex-1 p-6">
          {children}
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
      <DashboardShell>{children}</DashboardShell>
    </SidebarProvider>
  )
}
