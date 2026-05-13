'use client'
import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuth } from '@/features/auth/hooks/use-auth'
import {
  SidebarProvider,
  Sidebar,
  SidebarContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarFooter,
  SidebarInset,
} from '@/shared/components/ui/sidebar'
import { Home, CreditCard, ArrowLeftRight, Tag, BarChart3, LogOut } from 'lucide-react'
import Link from 'next/link'
import { Button } from '@/shared/components/ui/button'

const navItems = [
  { href: '/', label: 'Dashboard', icon: Home },
  { href: '/contas', label: 'Contas', icon: CreditCard },
  { href: '/transacoes', label: 'Transacoes', icon: ArrowLeftRight },
  { href: '/categorias', label: 'Categorias', icon: Tag },
  { href: '/relatorios', label: 'Relatorios', icon: BarChart3 },
]

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const auth = useAuth()
  const pathname = usePathname()

  useEffect(() => {
    if (!auth.loggedIn) {
      router.push('/login')
    }
  }, [auth.loggedIn, router])

  return (
    <SidebarProvider>
      <Sidebar>
        <SidebarHeader className="px-4 py-3">
          <span className="font-semibold text-lg">Financas Lab</span>
        </SidebarHeader>
        <SidebarContent>
          <SidebarMenu>
            {navItems.map((item) => {
              const isActive = item.href === '/'
                ? pathname === '/'
                : pathname.startsWith(item.href)

              return (
                <SidebarMenuItem key={item.href}>
                  <SidebarMenuButton
                    render={<Link href={item.href} />}
                    isActive={isActive}
                  >
                    <item.icon className="h-4 w-4" />
                    <span>{item.label}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )
            })}
          </SidebarMenu>
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
      </Sidebar>
      <SidebarInset>
        <main className="flex-1 p-6">
          {children}
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
