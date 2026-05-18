import { render, screen } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'

let mockIsMobile = false

vi.mock('next/navigation', () => ({
  usePathname: () => '/',
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('@/shared/components/ui/sidebar', () => ({
  useSidebar: () => ({ isMobile: mockIsMobile }),
  SidebarTrigger: () => <button>toggle</button>,
}))

vi.mock('@/features/auth/hooks/use-auth', () => ({
  useAuth: () => ({ logout: vi.fn() }),
}))

vi.mock('@/features/auth/hooks/use-current-user', () => ({
  useCurrentUser: () => ({ email: 'fabio@test.com', initials: 'F' }),
}))

import { ShellHeader } from './ShellHeader'
import { useTabsStore } from './tabs-store'

function makeTabs(count: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: `tab-${i}`,
    screenCode: 'REL-DSH-001',
    pinned: false,
  }))
}

describe('ShellHeader', () => {
  beforeEach(() => {
    mockIsMobile = false
    useTabsStore.setState({ tabs: [], activeId: null })
  })

  it('renderiza o SidebarTrigger', () => {
    render(<ShellHeader />)
    expect(screen.getByRole('button', { name: 'toggle' })).toBeInTheDocument()
  })

  it('mostra o titulo da tela ativa (pathname "/" -> Dashboard)', () => {
    render(<ShellHeader />)
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('em isMobile=true com 3 abas: mostra badge "3 abas"', () => {
    mockIsMobile = true
    useTabsStore.setState({ tabs: makeTabs(3), activeId: 'tab-0' })
    render(<ShellHeader />)
    expect(screen.getByText('3 abas')).toBeInTheDocument()
  })

  it('em isMobile=false: nao mostra badge mesmo com varias abas', () => {
    mockIsMobile = false
    useTabsStore.setState({ tabs: makeTabs(3), activeId: 'tab-0' })
    render(<ShellHeader />)
    expect(screen.queryByText('3 abas')).not.toBeInTheDocument()
  })

  it('renderiza as iniciais do usuario no avatar', () => {
    render(<ShellHeader />)
    const avatar = screen.getByRole('button', { name: 'Menu do usuario' })
    expect(avatar).toBeInTheDocument()
    expect(avatar).toHaveTextContent('F')
  })

  it('nao renderiza o botao de logout diretamente (esta dentro do dropdown)', () => {
    render(<ShellHeader />)
    expect(screen.queryByRole('menuitem', { name: /sair/i })).not.toBeInTheDocument()
  })
})
