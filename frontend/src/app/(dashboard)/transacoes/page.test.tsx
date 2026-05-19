import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/transacoes/services/transacoes.service', () => ({
  transacoesService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockReplace = vi.fn()
let currentParams = new URLSearchParams()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace }),
  usePathname: () => '/transacoes',
  useSearchParams: () => currentParams,
}))

vi.mock('@/features/auditlog', () => ({
  AuditLogDrawer: () => null,
}))

import TransacoesPage from './page'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import type { Transacao } from '@/features/transacoes/types/transacao'
import type { PageResponse } from '@/shared/hooks/useListPage'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const transacaoFixture = (overrides?: Partial<Transacao>): Transacao => ({
  id: 'tx-001',
  tipo: 'DESPESA',
  valor: 150.0,
  moeda: 'BRL',
  data: '2026-05-13',
  descricao: 'Supermercado',
  contaId: 'conta-001',
  contaDestinoId: null,
  categoriaId: null,
  criadoEm: '2026-05-13T00:00:00Z',
  atualizadoEm: '2026-05-13T00:00:00Z',
  status: 'CLEARED',
  payeeId: null,
  tagIds: [],
  transferGroupId: null,
  ...overrides,
})

const pageFixture = (
  content: Transacao[],
  overrides?: Partial<PageResponse<Transacao>>,
): PageResponse<Transacao> => ({
  content,
  totalElements: content.length,
  totalPages: content.length > 0 ? 1 : 0,
  number: 0,
  size: 20,
  ...overrides,
})

describe('TransacoesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    currentParams = new URLSearchParams()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(transacoesService.listar).mockReturnValue(new Promise(() => {}))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /transacoes/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe lista de transacoes apos carregamento', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(
      pageFixture([
        transacaoFixture({ tipo: 'DESPESA', descricao: 'Supermercado', valor: 150.0 }),
        transacaoFixture({ id: 'tx-002', tipo: 'RECEITA', descricao: 'Salario', valor: 5000.0 }),
      ]),
    )

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText('Salario')).toBeTruthy()
    expect(screen.getByText('Despesa')).toBeTruthy()
    expect(screen.getByText('Receita')).toBeTruthy()
  })

  it('exibe coluna Status com badge correto para CLEARED', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(
      pageFixture([transacaoFixture({ status: 'CLEARED' })]),
    )

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText('Confirmada')).toBeTruthy()
  })

  it('exibe coluna Status com badge correto para PENDING', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(
      pageFixture([transacaoFixture({ status: 'PENDING' })]),
    )

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText('Pendente')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero transacoes', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(pageFixture([]))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma transacao cadastrada/i)).toBeTruthy()
    })
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(transacoesService.listar).mockRejectedValue(new Error('Network error'))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar transacoes/i)).toBeTruthy()
    })
  })

  it('navega para /transacoes/novo ao clicar em Nova Transacao', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(pageFixture([]))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova transacao/i }))

    expect(mockPush).toHaveBeenCalledWith('/transacoes/novo')
  })

  it('exibe o indicador de paginacao quando ha registros', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(
      pageFixture([transacaoFixture()], { totalElements: 25, totalPages: 2 }),
    )

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/pagina 1 de 2/i)).toBeTruthy()
    })

    expect(screen.getByText(/25 transacao/i)).toBeTruthy()
  })

  it('formata valor da transacao em BRL', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(
      pageFixture([transacaoFixture({ valor: 1500.5 })]),
    )

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText(/1\.500,50/)).toBeTruthy()
  })

  it('exibe a barra de filtros', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue(pageFixture([]))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^filtro$/i })).toBeTruthy()
    })
  })
})
