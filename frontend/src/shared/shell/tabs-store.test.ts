import { describe, it, expect, beforeEach } from 'vitest'
import { useTabsStore, MAX_TABS, DASHBOARD_CODE } from './tabs-store'

/** Codigos de tela validos usados nos cenarios. */
const CODES = [
  'REL-DSH-001',
  'FIN-CTA-001',
  'FIN-TRX-001',
  'FIN-IMP-001',
  'CAD-CAT-001',
  'CAD-TAG-001',
  'FIN-ORC-001',
  'FIN-MET-001',
  'FIN-REC-001',
  'CAD-BEN-001',
  'REL-ANO-001',
  'REL-ANL-001',
]

describe('useTabsStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useTabsStore.setState({ tabs: [], activeId: null })
  })

  it('openTab cria nova aba com o screenCode e a ativa', () => {
    useTabsStore.getState().openTab('FIN-CTA-001')
    const { tabs, activeId } = useTabsStore.getState()
    expect(tabs).toHaveLength(1)
    expect(tabs[0].screenCode).toBe('FIN-CTA-001')
    expect(tabs[0].pinned).toBe(false)
    expect(activeId).toBe(tabs[0].id)
  })

  it('openTab com screenCode ja aberto apenas ativa (nao duplica)', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    const firstId = useTabsStore.getState().tabs[0].id
    openTab('FIN-CTA-001')
    const { tabs, activeId } = useTabsStore.getState()
    expect(tabs).toHaveLength(2)
    expect(activeId).toBe(firstId)
  })

  it('openTab respeita o limite de 10 abas removendo a mais antiga nao-fixada', () => {
    const { openTab } = useTabsStore.getState()
    for (let i = 0; i < MAX_TABS; i += 1) {
      openTab(CODES[i])
    }
    expect(useTabsStore.getState().tabs).toHaveLength(MAX_TABS)
    const oldestCode = useTabsStore.getState().tabs[0].screenCode
    openTab(CODES[MAX_TABS]) // 11a aba
    const { tabs } = useTabsStore.getState()
    expect(tabs).toHaveLength(MAX_TABS)
    expect(tabs.some((tab) => tab.screenCode === oldestCode)).toBe(false)
    expect(tabs.some((tab) => tab.screenCode === CODES[MAX_TABS])).toBe(true)
  })

  it('openTab com limite atingido e todas pinadas ignora silenciosamente', () => {
    const { openTab, togglePin } = useTabsStore.getState()
    for (let i = 0; i < MAX_TABS; i += 1) {
      openTab(CODES[i])
    }
    useTabsStore.getState().tabs.forEach((tab) => togglePin(tab.id))
    expect(useTabsStore.getState().tabs.every((tab) => tab.pinned)).toBe(true)
    openTab(CODES[MAX_TABS])
    const { tabs } = useTabsStore.getState()
    expect(tabs).toHaveLength(MAX_TABS)
    expect(tabs.some((tab) => tab.screenCode === CODES[MAX_TABS])).toBe(false)
  })

  it('closeTab remove a aba e ativa a anterior', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    openTab('FIN-ORC-001')
    const { tabs } = useTabsStore.getState()
    // Fecha a aba ativa (a ultima): ativa deve cair na vizinha a esquerda.
    useTabsStore.getState().closeTab(tabs[2].id)
    const after = useTabsStore.getState()
    expect(after.tabs).toHaveLength(2)
    expect(after.activeId).toBe(tabs[1].id)
  })

  it('closeTab da ultima aba reabre o Dashboard', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    const id = useTabsStore.getState().tabs[0].id
    useTabsStore.getState().closeTab(id)
    const { tabs, activeId } = useTabsStore.getState()
    expect(tabs).toHaveLength(1)
    expect(tabs[0].screenCode).toBe(DASHBOARD_CODE)
    expect(activeId).toBe(tabs[0].id)
    expect(activeId).not.toBeNull()
  })

  it('closeTab da unica aba Dashboard reabre o Dashboard (novo id)', () => {
    const { openTab } = useTabsStore.getState()
    openTab(DASHBOARD_CODE)
    const oldId = useTabsStore.getState().tabs[0].id
    useTabsStore.getState().closeTab(oldId)
    const { tabs, activeId } = useTabsStore.getState()
    expect(tabs).toHaveLength(1)
    expect(tabs[0].screenCode).toBe(DASHBOARD_CODE)
    expect(tabs[0].id).not.toBe(oldId)
    expect(activeId).toBe(tabs[0].id)
  })

  it('togglePin alterna o estado pinned', () => {
    useTabsStore.getState().openTab('FIN-CTA-001')
    const id = useTabsStore.getState().tabs[0].id
    useTabsStore.getState().togglePin(id)
    expect(useTabsStore.getState().tabs[0].pinned).toBe(true)
    useTabsStore.getState().togglePin(id)
    expect(useTabsStore.getState().tabs[0].pinned).toBe(false)
  })

  it('reorder move a aba para a nova posicao', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    openTab('FIN-ORC-001')
    useTabsStore.getState().reorder(0, 2)
    const codes = useTabsStore.getState().tabs.map((tab) => tab.screenCode)
    expect(codes).toEqual(['FIN-TRX-001', 'FIN-ORC-001', 'FIN-CTA-001'])
  })

  it('duplicateTab cria nova aba com mesmo screenCode logo apos a original', () => {
    const { openTab } = useTabsStore.getState()
    openTab('FIN-CTA-001')
    openTab('FIN-TRX-001')
    const originalId = useTabsStore.getState().tabs[0].id
    useTabsStore.getState().duplicateTab(originalId)
    const { tabs, activeId } = useTabsStore.getState()
    expect(tabs).toHaveLength(3)
    expect(tabs[1].screenCode).toBe('FIN-CTA-001')
    expect(tabs[1].id).not.toBe(originalId)
    expect(tabs[1].pinned).toBe(false)
    expect(activeId).toBe(tabs[1].id)
  })

  it('persiste tabs e activeId na chave financas-lab:tabs', () => {
    useTabsStore.getState().openTab('FIN-CTA-001')
    const raw = localStorage.getItem('financas-lab:tabs')
    expect(raw).not.toBeNull()
    const parsed = JSON.parse(raw as string)
    expect(parsed.state.tabs).toBeInstanceOf(Array)
    expect(parsed.state.tabs).toHaveLength(1)
    expect(parsed.state.activeId).toBe(useTabsStore.getState().activeId)
  })
})
