import { describe, it, expect, beforeEach } from 'vitest'
import { useSidebarStore } from './sidebar-store'

describe('useSidebarStore', () => {
  beforeEach(() => {
    localStorage.clear()
    // Resetar para o estado inicial real (grupos fechados por default)
    const { collapsed: initial } = useSidebarStore.getInitialState()
    useSidebarStore.setState({ collapsed: initial })
  })

  it('collapsed inicial inclui pelo menos um grupo de topo (Cadastros)', () => {
    const { collapsed } = useSidebarStore.getState()
    expect(collapsed).toContain('Cadastros')
  })

  it('collapsed inicial inclui subgrupos (Cadastros/Financeiro)', () => {
    const { collapsed } = useSidebarStore.getState()
    expect(collapsed).toContain('Cadastros/Financeiro')
  })

  it('collapsed inicial nao inclui folhas (codes de tela)', () => {
    const { collapsed } = useSidebarStore.getState()
    // Folhas nao tem / e nao sao grupos -- nenhum code deve aparecer
    expect(collapsed).not.toContain('FIN-CTA-001')
    expect(collapsed).not.toContain('Contas')
  })

  it('toggleGroup abre um grupo fechado', () => {
    // Inicia fechado
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
    useSidebarStore.getState().toggleGroup('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(false)
  })

  it('toggleGroup fecha um grupo aberto', () => {
    // Abrir primeiro
    useSidebarStore.getState().toggleGroup('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(false)
    // Fechar
    useSidebarStore.getState().toggleGroup('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
  })

  it('toggleGroup reabre um grupo fechado e fecha novamente', () => {
    const { toggleGroup } = useSidebarStore.getState()
    toggleGroup('Planejamento')
    toggleGroup('Planejamento')
    expect(useSidebarStore.getState().isCollapsed('Planejamento')).toBe(true)
  })

  it('abrir um grupo de topo fecha os irmaos de topo (accordion)', () => {
    const { toggleGroup } = useSidebarStore.getState()
    // Abrir Cadastros (grupo de topo).
    toggleGroup('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(false)
    // Abrir Movimento (irmao de topo): Cadastros deve fechar.
    toggleGroup('Movimento')
    expect(useSidebarStore.getState().isCollapsed('Movimento')).toBe(false)
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
  })

  it('accordion em subgrupos: abrir um subgrupo fecha o irmao sob o mesmo pai', () => {
    // Subgrupos irmaos sob 'Cadastros': 'Cadastros/Financeiro' e
    // 'Cadastros/Classificacao'. 'Movimento' (topo) nao deve ser afetado.
    const { toggleGroup } = useSidebarStore.getState()
    // Abrir o grupo pai e um subgrupo.
    toggleGroup('Cadastros')
    toggleGroup('Cadastros/Financeiro')
    expect(useSidebarStore.getState().isCollapsed('Cadastros/Financeiro')).toBe(false)
    // Abrir o subgrupo irmao: o primeiro deve fechar.
    toggleGroup('Cadastros/Classificacao')
    expect(useSidebarStore.getState().isCollapsed('Cadastros/Classificacao')).toBe(false)
    expect(useSidebarStore.getState().isCollapsed('Cadastros/Financeiro')).toBe(true)
    // Grupo de topo diferente permanece inalterado (fechado por default).
    expect(useSidebarStore.getState().isCollapsed('Movimento')).toBe(true)
  })

  it('fechar um grupo aberto so o adiciona ao collapsed (nao afeta irmaos)', () => {
    const { toggleGroup } = useSidebarStore.getState()
    // Abrir Cadastros e Movimento (accordion fecha Cadastros ao abrir Movimento).
    toggleGroup('Cadastros')
    toggleGroup('Movimento')
    expect(useSidebarStore.getState().isCollapsed('Movimento')).toBe(false)
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
    // Fechar Movimento: nao reabre Cadastros nem mexe em outros irmaos.
    toggleGroup('Movimento')
    expect(useSidebarStore.getState().isCollapsed('Movimento')).toBe(true)
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
    expect(useSidebarStore.getState().isCollapsed('Planejamento')).toBe(true)
  })

  it('chave localStorage e financas-lab:sidebar-v2', () => {
    useSidebarStore.getState().toggleGroup('Planejamento')
    const raw = localStorage.getItem('financas-lab:sidebar-v2')
    expect(raw).not.toBeNull()
    expect(JSON.parse(raw as string).state.collapsed).toBeInstanceOf(Array)
  })
})
