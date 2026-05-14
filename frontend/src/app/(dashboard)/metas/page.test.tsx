import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/metas/services/meta-service', () => ({
  metaService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import MetasPage from './page'
import { metaService } from '@/features/metas/services/meta-service'
import type { Meta } from '@/features/metas/types/meta'

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
  nome: 'Viagem Europa',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 3000, moeda: 'BRL' },
  prazo: '2027-12-31',
  status: 'EM_ANDAMENTO',
  atrasada: false,
  percentualConcluido: 30,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('MetasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(metaService.listar).mockReturnValue(new Promise(() => {}))

    render(<MetasPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /metas/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe lista de metas apos carregamento', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ nome: 'Viagem Europa', status: 'EM_ANDAMENTO' }),
      metaFixture({ id: 'meta-002', nome: 'Fundo de Emergencia', status: 'CONCLUIDA' }),
    ])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    expect(screen.getByText('Fundo de Emergencia')).toBeTruthy()
    expect(screen.getByText('Em Andamento')).toBeTruthy()
    expect(screen.getByText('Concluida')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero metas', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma meta cadastrada/i)).toBeTruthy()
    })
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(metaService.listar).mockRejectedValue(new Error('Network error'))

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar metas/i)).toBeTruthy()
    })
  })

  it('navega para /metas/novo ao clicar em Nova Meta', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova meta/i }))

    expect(mockPush).toHaveBeenCalledWith('/metas/novo')
  })

  it('navega para /metas/:id ao clicar em Ver', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ id: 'meta-001', nome: 'Viagem Europa' }),
    ])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /ver/i }))

    expect(mockPush).toHaveBeenCalledWith('/metas/meta-001')
  })

  it('exibe badge Atrasada quando meta esta atrasada e em andamento', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ atrasada: true, status: 'EM_ANDAMENTO' }),
    ])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Atrasada')).toBeTruthy()
    })
  })

  it('nao exibe badge Atrasada para meta concluida mesmo que atrasada seja true', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ atrasada: true, status: 'CONCLUIDA' }),
    ])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Concluida')).toBeTruthy()
    })

    expect(screen.queryByText('Atrasada')).toBeNull()
  })

  it('formata valor alvo em BRL', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ valorAlvo: { valor: 10000, moeda: 'BRL' } }),
    ])

    render(<MetasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/10\.000/)).toBeTruthy()
    })
  })
})
