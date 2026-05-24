import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('../services/meta-service', () => ({
  metaService: {
    listar: vi.fn(),
  },
}))

import { useMetasEmAndamento } from './useMetasEmAndamento'
import { metaService } from '../services/meta-service'
import type { Meta } from '../types/meta'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const metaFixture = (overrides?: Partial<Meta>): Meta => ({
  id: 'meta-001',
  nome: 'Viagem',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 2000, moeda: 'BRL' },
  prazo: '2030-01-01',
  status: 'EM_ANDAMENTO',
  atrasada: false,
  percentualConcluido: 20,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

describe('useMetasEmAndamento', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(metaService.listar).mockResolvedValue([])
  })

  it('retorna lista vazia quando nao ha metas', async () => {
    const { result } = renderHook(() => useMetasEmAndamento(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.metas).toEqual([])
  })

  it('filtra metas com status diferente de EM_ANDAMENTO', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ status: 'CONCLUIDA' }),
      metaFixture({ id: 'm2', status: 'CANCELADA' }),
      metaFixture({ id: 'm3', status: 'EM_ANDAMENTO' }),
    ])
    const { result } = renderHook(() => useMetasEmAndamento(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.metas).toHaveLength(1))
    expect(result.current.metas[0].id).toBe('m3')
  })
})
