/**
 * ShellHeader -- barra superior do shell (ADR-014 fase UI-3, spec secao 4.4).
 *
 * Mostra o toggle da sidebar, o nome da aplicacao, o titulo da tela ativa,
 * no mobile um badge com a contagem de abas abertas e um avatar de perfil
 * com dropdown (email do usuario logado + opcao de sair). A classe
 * `shell-header` no `<header>` e consumida pelos print styles em
 * `globals.css` para esconder o shell na impressao.
 */
'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Bell, LogOut, X } from 'lucide-react'
import { Separator } from '@/shared/components/ui/separator'
import { SidebarTrigger, useSidebar } from '@/shared/components/ui/sidebar'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuItem,
} from '@/shared/components/ui/dropdown-menu'
import { useAuth } from '@/features/auth/hooks/use-auth'
import { useCurrentUser } from '@/features/auth/hooks/use-current-user'
import { useNotificacoes } from '@/shared/hooks/useNotificacoes'
import { useTabsStore } from './tabs-store'

export function ShellHeader() {
  const router = useRouter()
  const { isMobile } = useSidebar()
  const { logout } = useAuth()
  const { email, initials } = useCurrentUser()
  const tabCount = useTabsStore((state) => state.tabs.length)
  const { notificacoes } = useNotificacoes()

  // IDs dispensados na sessao atual (reseta ao recarregar a pagina)
  const [dispensados, setDispensados] = useState<Set<string>>(new Set())
  const visiveis = notificacoes.filter((n) => !dispensados.has(n.id))
  const count = visiveis.length

  function dispensar(id: string) {
    setDispensados((prev) => new Set([...prev, id]))
  }

  function dispensarTodas() {
    setDispensados(new Set(notificacoes.map((n) => n.id)))
  }

  const handleLogout = () => {
    logout()
    router.push('/login')
  }

  return (
    <header className="shell-header flex h-12 shrink-0 items-center gap-2 border-b border-border bg-background px-4">
      <SidebarTrigger className="-ml-1" />
      <Separator orientation="vertical" className="h-4" />
      <span className="text-xs font-semibold text-muted-foreground tracking-wide uppercase select-none">
        Financas Lab
      </span>
      <div className="ml-auto flex items-center gap-2">
        {isMobile && tabCount > 1 && (
          <span className="rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
            {tabCount} abas
          </span>
        )}
        {count > 0 && (
          <DropdownMenu>
            <DropdownMenuTrigger
              render={
                <button
                  type="button"
                  aria-label={`${count} notificacoes ativas`}
                  className="relative flex items-center justify-center rounded p-1 hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                />
              }
            >
              <Bell className="h-5 w-5 text-muted-foreground" />
              <span className="absolute top-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-destructive text-[10px] font-bold text-white">
                {count > 9 ? '9+' : count}
              </span>
            </DropdownMenuTrigger>
            <DropdownMenuContent side="bottom" align="end" className="w-80">
              <DropdownMenuGroup>
                <DropdownMenuLabel>Notificacoes</DropdownMenuLabel>
              </DropdownMenuGroup>
              <DropdownMenuSeparator />
              {visiveis.map((n) => (
                <div key={n.id} className="flex items-start gap-1 px-1 py-0.5">
                  <div className="flex flex-1 flex-col gap-0.5 px-1 py-1.5">
                    <span className="text-sm font-medium leading-none">{n.titulo}</span>
                    <span className="text-xs text-muted-foreground">{n.descricao}</span>
                  </div>
                  <button
                    type="button"
                    aria-label="Dispensar notificacao"
                    onClick={() => dispensar(n.id)}
                    className="mt-1 shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ))}
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={dispensarTodas}
                className="justify-center text-xs text-muted-foreground"
              >
                Dispensar todas
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <button
                type="button"
                aria-label="Menu do usuario"
                className="flex h-7 w-7 cursor-pointer items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            }
          >
            {initials}
          </DropdownMenuTrigger>
          <DropdownMenuContent side="bottom" align="end">
            {email && (
              <>
                <DropdownMenuGroup>
                  <DropdownMenuLabel className="max-w-48 truncate">
                    {email}
                  </DropdownMenuLabel>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
              </>
            )}
            <DropdownMenuItem
              onClick={handleLogout}
              className="cursor-pointer gap-2"
            >
              <LogOut className="h-4 w-4" />
              Sair
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
