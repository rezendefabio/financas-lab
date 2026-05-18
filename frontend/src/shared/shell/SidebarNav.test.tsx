import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'

let mockPathname = '/'
vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}))

import { SidebarNav } from './SidebarNav'
import { SidebarProvider } from '@/shared/components/ui/sidebar'
import { useSidebarStore } from './sidebar-store'
import { useTabsStore } from './tabs-store'

function renderNav() {
  return render(
    <SidebarProvider>
      <SidebarNav />
    </SidebarProvider>,
  )
}

describe('SidebarNav', () => {
  beforeEach(() => {
    mockPathname = '/'
    localStorage.clear()
    // Abrir todos os grupos para que os testes de renderizacao de itens funcionem
    useSidebarStore.setState({ collapsed: [] })
    useTabsStore.setState({ tabs: [], activeId: null })
  })

  it('renderiza os grupos de topo do menuPath', () => {
    renderNav()
    expect(screen.getByText('Cadastros')).toBeInTheDocument()
    expect(screen.getByText('Movimento')).toBeInTheDocument()
    expect(screen.getByText('Planejamento')).toBeInTheDocument()
    expect(screen.getByText('Analise')).toBeInTheDocument()
  })

  it('renderiza as telas folha dentro dos grupos', () => {
    renderNav()
    expect(screen.getByText('Contas')).toBeInTheDocument()
    expect(screen.getByText('Transacoes')).toBeInTheDocument()
    expect(screen.getByText('Relatorios')).toBeInTheDocument()
  })

  it('destaca o item ativo conforme o pathname', () => {
    mockPathname = '/contas'
    renderNav()
    // base-nova emite data-active="" quando ativo e omite o atributo quando nao.
    // SidebarMenuSubButton mantem a tag <a> (defaultTagName) mesmo apos a
    // UI-2 trocar a navegacao por onClick.
    const contasLink = screen.getByText('Contas').closest('a')
    expect(contasLink?.hasAttribute('data-active')).toBe(true)
    const tagsLink = screen.getByText('Tags').closest('a')
    expect(tagsLink?.hasAttribute('data-active')).toBe(false)
  })

  it('destaca o grupo do item ativo (breadcrumb visual)', () => {
    mockPathname = '/contas'
    renderNav()
    const grupo = screen.getByText('Cadastros').closest('button')
    expect(grupo?.hasAttribute('data-active')).toBe(true)
    const outroGrupo = screen.getByText('Analise').closest('button')
    expect(outroGrupo?.hasAttribute('data-active')).toBe(false)
  })

  it('destaca o item ativo tambem em rota filha', () => {
    mockPathname = '/contas/novo'
    renderNav()
    const contasLink = screen.getByText('Contas').closest('a')
    expect(contasLink?.hasAttribute('data-active')).toBe(true)
  })

  it('clicar numa tela folha abre uma aba no Tab Manager', async () => {
    renderNav()
    expect(useTabsStore.getState().tabs).toHaveLength(0)
    await userEvent.click(screen.getByText('Contas'))
    const { tabs } = useTabsStore.getState()
    expect(tabs).toHaveLength(1)
    expect(tabs[0].screenCode).toBe('FIN-CTA-001')
  })

  it('colapsa um grupo ao clicar nele', async () => {
    renderNav()
    expect(screen.getByText('Contas')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Cadastros'))
    expect(screen.queryByText('Contas')).not.toBeInTheDocument()
  })

  // --- Busca fixa ---

  it('campo de busca renderiza com placeholder "Buscar tela..."', () => {
    renderNav()
    expect(
      screen.getByPlaceholderText('Buscar tela...'),
    ).toBeInTheDocument()
  })

  it('digitar "Contas" no campo exibe item Contas', async () => {
    renderNav()
    await userEvent.type(screen.getByPlaceholderText('Buscar tela...'), 'Contas')
    expect(screen.getByText('Contas')).toBeInTheDocument()
  })

  it('digitar "FIN-CTA" exibe item Contas (busca por codigo)', async () => {
    renderNav()
    await userEvent.type(screen.getByPlaceholderText('Buscar tela...'), 'FIN-CTA')
    expect(screen.getByText('Contas')).toBeInTheDocument()
  })

  it('digitar "xyzxyz" exibe mensagem "Nenhuma tela encontrada."', async () => {
    renderNav()
    await userEvent.type(screen.getByPlaceholderText('Buscar tela...'), 'xyzxyz')
    expect(screen.getByText('Nenhuma tela encontrada.')).toBeInTheDocument()
  })

  it('limpar o campo restaura a view normal com grupos visiveis', async () => {
    renderNav()
    const input = screen.getByPlaceholderText('Buscar tela...')
    await userEvent.type(input, 'xyzxyz')
    expect(screen.getByText('Nenhuma tela encontrada.')).toBeInTheDocument()
    await userEvent.clear(input)
    expect(screen.queryByText('Nenhuma tela encontrada.')).not.toBeInTheDocument()
    expect(screen.getByText('Cadastros')).toBeInTheDocument()
  })
})
