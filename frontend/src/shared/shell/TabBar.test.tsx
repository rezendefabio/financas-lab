import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'

const mockReplace = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: mockReplace }),
  usePathname: () => '/',
}))

import { TabBar } from './TabBar'
import { useTabsStore } from './tabs-store'
import { useCommandPaletteStore } from './command-palette-store'

describe('TabBar', () => {
  beforeEach(() => {
    localStorage.clear()
    mockReplace.mockClear()
    useTabsStore.setState({ tabs: [], activeId: null })
    useCommandPaletteStore.setState({ open: false })
  })

  it('sem abas nao renderiza a faixa', () => {
    const { container } = render(<TabBar />)
    expect(container).toBeEmptyDOMElement()
  })

  it('com duas abas renderiza ambas e destaca a ativa', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    render(<TabBar />)
    expect(screen.getAllByText('Contas').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Transacoes').length).toBeGreaterThan(0)
    // A aba ativa (Transacoes, ultima aberta) tem aria-selected.
    const tabs = screen.getAllByRole('tab')
    const active = tabs.filter(
      (tab) => tab.getAttribute('aria-selected') === 'true',
    )
    expect(active).toHaveLength(1)
  })

  it('clicar no X de uma aba a fecha', async () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    render(<TabBar />)
    await userEvent.click(screen.getByLabelText('Fechar aba Contas'))
    const { tabs } = useTabsStore.getState()
    expect(tabs).toHaveLength(1)
    expect(tabs[0].screenCode).toBe('FIN-TRX-001')
  })

  it('clicar no corpo da aba a ativa', async () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    const contasId = useTabsStore.getState().tabs[0].id
    render(<TabBar />)
    // Aba ativa inicial e Transacoes; clicar em Contas troca a ativa.
    await userEvent.click(screen.getAllByText('Contas')[0])
    expect(useTabsStore.getState().activeId).toBe(contasId)
  })

  it('aba fixada mostra icone de pin no lugar do X', () => {
    const { openTab, togglePin } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    const id = useTabsStore.getState().tabs[0].id
    togglePin(id)
    render(<TabBar />)
    expect(screen.getByLabelText('Desfixar aba Contas')).toBeInTheDocument()
    expect(screen.queryByLabelText('Fechar aba Contas')).not.toBeInTheDocument()
  })

  it('sincroniza a URL com a rota da aba ativa sem expor tabs/active', () => {
    // O estado das abas vive no localStorage; a URL reflete apenas a rota da
    // aba ativa, sem os parametros ?tabs=...&active=... (que antes expunham e
    // tornavam o estado manipulavel).
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')

    render(<TabBar />)

    expect(mockReplace).toHaveBeenCalled()
    const url = mockReplace.mock.calls.at(-1)![0] as string
    expect(url).toBe('/transacoes')
    expect(url).not.toContain('tabs=')
    expect(url).not.toContain('active=')
  })

  it('saneia tabs/active residuais do currentPath preservando params de pagina', () => {
    // currentPath vindo de uma URL antiga (versao que espelhava o estado das
    // abas) ainda pode conter ?tabs=...&active=...; o sync remove esses e
    // mantem os params legitimos da pagina (ex: page=2).
    useTabsStore.setState({
      tabs: [
        {
          id: 't1',
          screenCode: 'FIN-CTA-001',
          pinned: false,
          currentPath: '/contas?tabs=FIN-CTA-001&active=FIN-CTA-001&page=2',
        },
      ],
      activeId: 't1',
    })

    render(<TabBar />)

    const url = mockReplace.mock.calls.at(-1)![0] as string
    expect(url).not.toContain('tabs=')
    expect(url).not.toContain('active=')
    expect(url).toContain('page=2')
  })

  it('botao "+" abre o CommandPalette', async () => {
    useTabsStore.getState().openTab('FIN-CTA-001')
    render(<TabBar />)
    expect(useCommandPaletteStore.getState().open).toBe(false)
    await userEvent.click(screen.getByLabelText('Abrir nova aba'))
    expect(useCommandPaletteStore.getState().open).toBe(true)
  })
})
