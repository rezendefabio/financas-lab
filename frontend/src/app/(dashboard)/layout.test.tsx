import { render } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'

let mockPathname = '/incidentes'
let mockSearchParams = new URLSearchParams()
vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
  useSearchParams: () => mockSearchParams,
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

import { TabPathTracker } from './layout'
import { useTabsStore } from '@/shared/shell'
import { useDraftFormsStore } from '@/shared/shell/draft-forms-store'

describe('TabPathTracker', () => {
  beforeEach(() => {
    localStorage.clear()
    mockPathname = '/incidentes'
    mockSearchParams = new URLSearchParams()
    useTabsStore.setState({ tabs: [], activeId: null })
    useDraftFormsStore.setState({ drafts: {} })
  })

  it('persiste fullPath com search params quando searchParams mudam na mesma aba', () => {
    useTabsStore.getState().openTab('FIN-TRX-001')
    const tabId = useTabsStore.getState().tabs[0].id
    useTabsStore.getState().updateTabPath(tabId, '/incidentes')

    mockPathname = '/incidentes'
    const { rerender } = render(<TabPathTracker />)

    mockSearchParams = new URLSearchParams('classeErro=NullPointer&submitted=1')
    rerender(<TabPathTracker />)

    const tab = useTabsStore.getState().tabs.find((t) => t.id === tabId)
    expect(tab?.currentPath).toBe(
      '/incidentes?classeErro=NullPointer&submitted=1',
    )
  })

  it('preserva rascunho ao trocar de aba mesmo com search params presentes', () => {
    useTabsStore.getState().openTab('FIN-TRX-001')
    useTabsStore.getState().openTab('FIN-CTA-001')
    const [tab1, tab2] = useTabsStore.getState().tabs
    useTabsStore.setState({ activeId: tab1.id })

    useDraftFormsStore
      .getState()
      .save('/incidentes?classeErro=NullPointer', { foo: 'bar' })

    mockPathname = '/incidentes'
    mockSearchParams = new URLSearchParams('classeErro=NullPointer')
    const { rerender } = render(<TabPathTracker />)

    useTabsStore.setState({ activeId: tab2.id })
    mockPathname = '/contas'
    mockSearchParams = new URLSearchParams()
    rerender(<TabPathTracker />)

    expect(
      useDraftFormsStore
        .getState()
        .getDraft('/incidentes?classeErro=NullPointer'),
    ).toEqual({ foo: 'bar' })
  })

  it('descarta rascunho ao trocar apenas params (mesmo pathname) dentro da aba', () => {
    useTabsStore.getState().openTab('FIN-TRX-001')

    useDraftFormsStore
      .getState()
      .save('/transacoes?categoria=id1', { foo: 'bar' })

    mockPathname = '/transacoes'
    mockSearchParams = new URLSearchParams('categoria=id1')
    const { rerender } = render(<TabPathTracker />)

    mockSearchParams = new URLSearchParams('categoria=id2')
    rerender(<TabPathTracker />)

    expect(
      useDraftFormsStore.getState().getDraft('/transacoes?categoria=id1'),
    ).toBeNull()
    const tabId = useTabsStore.getState().activeId
    const tab = useTabsStore.getState().tabs.find((t) => t.id === tabId)
    expect(tab?.currentPath).toBe('/transacoes?categoria=id2')
  })
})
