import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/orcamentos/services/orcamento-service', () => ({
  orcamentoService: {
    listar: vi.fn(),
  },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import OrcamentosPage from './page'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import type { Orcamento } from '@/features/orcamentos/types/orcamento'
import type { Categoria } from '@/features/categorias/types/categoria'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const orcamentoFixture = (overrides?: Partial<Orcamento>): Orcamento => ({
  id: 'orc-001',
  categoriaId: 'cat-001',
  valorLimite: { valor: 500, moeda: 'BRL' },
  mesAno: '2024-05-01',
  ativo: true,
  criadoEm: '2024-05-01T00:00:00Z',
  atualizadoEm: '2024-05-01T00:00:00Z',
  ...overrides,
})

const categoriaFixture = (overrides?: Partial<Categoria>): Categoria => ({
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  criadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

describe('OrcamentosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(categoriasService.listar).mockResolvedValue([])
  })

  it('exibe skeleton durante carregamento', () => {
    vi.mocked(orcamentoService.listar).mockReturnValue(new Promise(() => {}))

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /orcamentos/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe mensagem vazia quando lista retorna zero orcamentos', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum orcamento cadastrado/i)).toBeTruthy()
    })
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(orcamentoService.listar).mockRejectedValue(new Error('Network error'))

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar orcamentos/i)).toBeTruthy()
    })
  })

  it('exibe tabela com dados quando ha orcamentos', async () => {
    vi.mocked(categoriasService.listar).mockResolvedValue([
      categoriaFixture({ id: 'cat-001', nome: 'Alimentacao' }),
    ])
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ id: 'orc-001', categoriaId: 'cat-001', mesAno: '2024-05-01', valorLimite: { valor: 500, moeda: 'BRL' }, ativo: true }),
    ])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })

    expect(screen.getByText('05/2024')).toBeTruthy()
    expect(screen.getByText(/500/)).toBeTruthy()
    expect(screen.getByText('Ativo')).toBeTruthy()
  })

  it('exibe categoriaId quando categoria nao encontrada', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ categoriaId: 'cat-desconhecida' }),
    ])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('cat-desconhecida')).toBeTruthy()
    })
  })

  it('exibe badge Inativo para orcamento inativo', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ ativo: false }),
    ])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Inativo')).toBeTruthy()
    })
  })

  it('navega para /orcamentos/novo ao clicar no botao de novo orcamento', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /novo orcamento/i }))

    expect(mockPush).toHaveBeenCalledWith('/orcamentos/novo')
  })

  it('navega para detalhe ao clicar em Ver', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ id: 'orc-001' }),
    ])

    render(<OrcamentosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /ver/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /ver/i }))

    expect(mockPush).toHaveBeenCalledWith('/orcamentos/orc-001')
  })
})
