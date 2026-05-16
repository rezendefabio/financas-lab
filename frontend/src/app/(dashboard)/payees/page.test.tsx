import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/payee/services/payee-service', () => ({
  listarPayees: vi.fn(),
  deletarPayee: vi.fn(),
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

import PayeesPage from './page'
import { listarPayees, deletarPayee } from '@/features/payee/services/payee-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import type { Payee } from '@/features/payee/types/payee'
import type { Categoria } from '@/features/categorias/types/categoria'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const payeeFixture = (overrides?: Partial<Payee>): Payee => ({
  id: 'pay-001',
  userId: 'user-001',
  nome: 'Supermercado Extra',
  categoriaPadraoId: undefined,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

const categoriaFixture = (overrides?: Partial<Categoria>): Categoria => ({
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  criadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('PayeesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(categoriasService.listar).mockResolvedValue([])
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(listarPayees).mockReturnValue(new Promise(() => {}))

    render(<PayeesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /beneficiarios/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe cabecalhos de tabela Nome, Categoria Padrao e Acoes', async () => {
    vi.mocked(listarPayees).mockResolvedValue([payeeFixture()])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nome')).toBeTruthy()
    })
    expect(screen.getByText('Categoria Padrao')).toBeTruthy()
    expect(screen.getByText('Acoes')).toBeTruthy()
  })

  it('exibe lista de payees apos carregamento', async () => {
    vi.mocked(listarPayees).mockResolvedValue([
      payeeFixture({ nome: 'Supermercado Extra' }),
      payeeFixture({ id: 'pay-002', nome: 'Farmacia Pague Menos' }),
    ])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado Extra')).toBeTruthy()
    })
    expect(screen.getByText('Farmacia Pague Menos')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero payees', async () => {
    vi.mocked(listarPayees).mockResolvedValue([])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum beneficiario cadastrado/i)).toBeTruthy()
    })
    expect(screen.getByRole('button', { name: /criar primeiro beneficiario/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarPayees).mockRejectedValue(new Error('Network error'))

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar beneficiarios/i)).toBeTruthy()
    })
  })

  it('navega para /payees/novo ao clicar em + Novo Beneficiario', async () => {
    vi.mocked(listarPayees).mockResolvedValue([])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /novo beneficiario/i }))

    expect(mockPush).toHaveBeenCalledWith('/payees/novo')
  })

  it('navega para /payees/:id/editar ao clicar em Editar', async () => {
    vi.mocked(listarPayees).mockResolvedValue([payeeFixture({ id: 'pay-001', nome: 'Teste' })])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Teste')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/payees/pay-001/editar')
  })

  it('exibe nome da categoria quando payee tem categoriaPadraoId', async () => {
    vi.mocked(listarPayees).mockResolvedValue([
      payeeFixture({ categoriaPadraoId: 'cat-001' }),
    ])
    vi.mocked(categoriasService.listar).mockResolvedValue([
      categoriaFixture({ id: 'cat-001', nome: 'Alimentacao' }),
    ])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })
  })

  it('exibe traco quando payee nao tem categoria padrao', async () => {
    vi.mocked(listarPayees).mockResolvedValue([
      payeeFixture({ categoriaPadraoId: undefined }),
    ])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado Extra')).toBeTruthy()
    })
    expect(screen.getByText('—')).toBeTruthy()
  })

  it('exibe botoes de confirmacao ao clicar em Excluir', async () => {
    vi.mocked(listarPayees).mockResolvedValue([payeeFixture()])

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /excluir/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('chama deletarPayee ao confirmar exclusao', async () => {
    vi.mocked(listarPayees).mockResolvedValue([payeeFixture({ id: 'pay-001' })])
    vi.mocked(deletarPayee).mockResolvedValue(undefined)

    render(<PayeesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /excluir/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(deletarPayee).toHaveBeenCalledWith('pay-001')
    })
  })
})
