import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'

let mockIsMobile = false
let mockCurrentUser: { email: string | null; initials: string } = {
  email: 'fabio@test.com',
  initials: 'F',
}
const mockLogout = vi.fn()
const mockPush = vi.fn()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

vi.mock('@/shared/components/ui/sidebar', () => ({
  useSidebar: () => ({ isMobile: mockIsMobile }),
  SidebarTrigger: () => <button>toggle</button>,
}))

vi.mock('@/features/auth/hooks/use-auth', () => ({
  useAuth: () => ({ logout: mockLogout }),
}))

vi.mock('@/features/auth/hooks/use-current-user', () => ({
  useCurrentUser: () => mockCurrentUser,
}))

let mockNotificacoes: Array<{ id: string; tipo: string; titulo: string; descricao: string }> = []

vi.mock('@/shared/hooks/useNotificacoes', () => ({
  useNotificacoes: () => ({ notificacoes: mockNotificacoes, isLoading: false }),
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
    mockCurrentUser = { email: 'fabio@test.com', initials: 'F' }
    mockNotificacoes = []
    mockLogout.mockClear()
    mockPush.mockClear()
    useTabsStore.setState({ tabs: [], activeId: null })
  })

  it('renderiza o SidebarTrigger', () => {
    render(<ShellHeader />)
    expect(screen.getByRole('button', { name: 'toggle' })).toBeInTheDocument()
  })

  it('mostra o nome da aplicacao no header', () => {
    render(<ShellHeader />)
    expect(screen.getByText('Financas Lab')).toBeInTheDocument()
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

  it('sem usuario logado: avatar mostra "?"', () => {
    mockCurrentUser = { email: null, initials: '?' }
    render(<ShellHeader />)
    const avatar = screen.getByRole('button', { name: 'Menu do usuario' })
    expect(avatar).toHaveTextContent('?')
  })

  it('nao renderiza o botao de logout diretamente (esta dentro do dropdown)', () => {
    render(<ShellHeader />)
    expect(screen.queryByRole('menuitem', { name: /sair/i })).not.toBeInTheDocument()
  })

  it('abrir o dropdown mostra o email do usuario no label', async () => {
    render(<ShellHeader />)
    await userEvent.click(screen.getByRole('button', { name: 'Menu do usuario' }))
    expect(await screen.findByText('fabio@test.com')).toBeInTheDocument()
  })

  it('sem usuario logado: dropdown aberto nao mostra label de email', async () => {
    mockCurrentUser = { email: null, initials: '?' }
    render(<ShellHeader />)
    await userEvent.click(screen.getByRole('button', { name: 'Menu do usuario' }))
    // Espera o dropdown abrir (item "Sair" presente) antes de afirmar ausencia.
    await screen.findByRole('menuitem', { name: /sair/i })
    expect(screen.queryByText(/@/)).not.toBeInTheDocument()
  })

  it('nao mostra badge de notificacoes quando lista esta vazia', () => {
    mockNotificacoes = []
    render(<ShellHeader />)
    expect(screen.queryByRole('status', { name: /notificacoes ativas/i })).not.toBeInTheDocument()
  })

  it('mostra badge com contagem quando ha notificacoes', () => {
    mockNotificacoes = [
      { id: 'n1', tipo: 'orcamento_excedido', titulo: 't', descricao: 'd' },
      { id: 'n2', tipo: 'meta_vencendo', titulo: 't', descricao: 'd' },
    ]
    render(<ShellHeader />)
    const badge = screen.getByRole('status', { name: /2 notificacoes ativas/i })
    expect(badge).toHaveTextContent('2')
  })

  it('mostra "9+" quando ha mais de 9 notificacoes', () => {
    mockNotificacoes = Array.from({ length: 12 }, (_, i) => ({
      id: `n${i}`,
      tipo: 'orcamento_excedido',
      titulo: 't',
      descricao: 'd',
    }))
    render(<ShellHeader />)
    const badge = screen.getByRole('status', { name: /12 notificacoes ativas/i })
    expect(badge).toHaveTextContent('9+')
  })

  it('clicar em "Meu Perfil" no dropdown navega para /perfil', async () => {
    render(<ShellHeader />)
    await userEvent.click(screen.getByRole('button', { name: 'Menu do usuario' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: /meu perfil/i }))
    expect(mockPush).toHaveBeenCalledWith('/perfil')
  })

  it('clicar em "Sair" no dropdown faz logout e navega para /login', async () => {
    render(<ShellHeader />)
    await userEvent.click(screen.getByRole('button', { name: 'Menu do usuario' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: /sair/i }))
    expect(mockLogout).toHaveBeenCalledTimes(1)
    expect(mockPush).toHaveBeenCalledWith('/login')
  })
})
