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
    useSidebarStore.setState({ collapsed: [] })
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

  it('colapsa um grupo ao clicar nele', async () => {
    renderNav()
    expect(screen.getByText('Contas')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Cadastros'))
    expect(screen.queryByText('Contas')).not.toBeInTheDocument()
  })
})
