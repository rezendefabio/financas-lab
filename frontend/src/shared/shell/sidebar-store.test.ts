import { describe, it, expect, beforeEach } from 'vitest'
import { useSidebarStore } from './sidebar-store'

describe('useSidebarStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useSidebarStore.setState({ collapsed: [] })
  })

  it('inicia com todos os grupos abertos (collapsed vazio)', () => {
    expect(useSidebarStore.getState().collapsed).toEqual([])
  })

  it('toggleGroup fecha um grupo aberto', () => {
    useSidebarStore.getState().toggleGroup('Cadastros')
    expect(useSidebarStore.getState().collapsed).toContain('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(true)
  })

  it('toggleGroup reabre um grupo fechado', () => {
    const { toggleGroup } = useSidebarStore.getState()
    toggleGroup('Cadastros')
    toggleGroup('Cadastros')
    expect(useSidebarStore.getState().collapsed).not.toContain('Cadastros')
    expect(useSidebarStore.getState().isCollapsed('Cadastros')).toBe(false)
  })

  it('toggle de grupos diferentes e independente', () => {
    const { toggleGroup } = useSidebarStore.getState()
    toggleGroup('Cadastros')
    toggleGroup('Movimento')
    expect(useSidebarStore.getState().collapsed.sort()).toEqual([
      'Cadastros',
      'Movimento',
    ])
  })

  it('persiste o estado de grupos colapsados em localStorage', () => {
    useSidebarStore.getState().toggleGroup('Planejamento')
    const raw = localStorage.getItem('financas-lab:sidebar')
    expect(raw).not.toBeNull()
    expect(JSON.parse(raw as string).state.collapsed).toContain('Planejamento')
  })
})
