import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement } from 'react'
import { useAuditLog } from './use-audit-log'
import type { AuditLogPage } from '../types/auditlog'

vi.mock('../services/auditlog-service', () => ({
  auditlogService: {
    listarPorEntidade: vi.fn(),
  },
}))

import { auditlogService } from '../services/auditlog-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return createElement(QueryClientProvider, { client }, children)
  }
}

const pagina = (overrides?: Partial<AuditLogPage>): AuditLogPage => ({
  content: [
    {
      id: 'log-1',
      entityType: 'conta',
      entityId: 'conta-1',
      action: 'CREATE',
      userEmail: 'user@exemplo.com',
      screenCode: 'FIN-CTA-001',
      before: null,
      after: '{}',
      criadoEm: '2026-05-18T10:00:00Z',
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  ...overrides,
})

describe('useAuditLog', () => {
  beforeEach(() => vi.clearAllMocks())

  it('fica desabilitado quando entityId e null', () => {
    const { result } = renderHook(() => useAuditLog('conta', null), {
      wrapper: makeWrapper(),
    })

    expect(result.current.isLoading).toBe(false)
    expect(result.current.entries).toEqual([])
    expect(auditlogService.listarPorEntidade).not.toHaveBeenCalled()
  })

  it('carrega as entradas quando entityId esta presente', async () => {
    vi.mocked(auditlogService.listarPorEntidade).mockResolvedValue(pagina())

    const { result } = renderHook(() => useAuditLog('conta', 'conta-1'), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => expect(result.current.entries).toHaveLength(1))
    expect(result.current.entries[0].id).toBe('log-1')
    expect(auditlogService.listarPorEntidade).toHaveBeenCalledWith('conta', 'conta-1', 0, 20)
  })

  it('expoe hasNextPage quando ha mais de uma pagina', async () => {
    vi.mocked(auditlogService.listarPorEntidade).mockResolvedValue(
      pagina({ totalPages: 3, totalElements: 50 }),
    )

    const { result } = renderHook(() => useAuditLog('conta', 'conta-1'), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => expect(result.current.hasNextPage).toBe(true))
  })
})
