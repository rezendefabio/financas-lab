'use client'
import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { isAuthenticated } from '@/lib/auth'
import { useAuth } from '@/hooks/use-auth'
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
} from '@/components/ui/sidebar'
import { Home, CreditCard, ArrowLeftRight, Tag, BarChart3, LogOut } from 'lucide-react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'

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

  useEffect(() => {
    if (!isAuthenticated()) {
      router.push('/login')
    }
  }, [router])

  return (
    <SidebarProvider>
      <Sidebar>
        <SidebarHeader className="px-4 py-3">
          <span className="font-semibold text-lg">Financas Lab</span>
        </SidebarHeader>
        <SidebarContent>
          <SidebarMenu>
            {navItems.map((item) => (
              <SidebarMenuItem key={item.href}>
                <SidebarMenuButton render={<Link href={item.href} />}>
                  <item.icon className="h-4 w-4" />
                  <span>{item.label}</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            ))}
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
