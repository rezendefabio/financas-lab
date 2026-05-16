import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
    buscar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockParams = { id: 'cat-001' }
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => mockParams,
}))

import CategoriaDetalhePage from './page'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import type { Categoria } from '@/features/categorias/types/categoria'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const categoriaFixture = (overrides?: Partial<Categoria>): Categoria => ({
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  system: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-02-01T00:00:00Z',
  ...overrides,
})

describe('CategoriaDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(categoriasService.buscar).mockReturnValue(new Promise(() => {}))
    vi.mocked(categoriasService.listar).mockReturnValue(new Promise(() => {}))

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(categoriasService.buscar).mockRejectedValue(new Error('Not found'))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar categoria/i)).toBeTruthy()
    })
  })

  it('exibe campos da categoria apos carregamento', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture())
    vi.mocked(categoriasService.listar).mockResolvedValue([categoriaFixture()])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })

    expect(screen.getByText('Nome')).toBeTruthy()
    expect(screen.getByText('Tipo')).toBeTruthy()
    expect(screen.getByText('Categoria Pai')).toBeTruthy()
    expect(screen.getByText('Sistema')).toBeTruthy()
    expect(screen.getByText('Criado em')).toBeTruthy()
    expect(screen.getByText('Atualizado em')).toBeTruthy()
  })

  it('exibe badge Despesa para tipo DESPESA', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ tipo: 'DESPESA' }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Despesa')).toBeTruthy()
    })
  })

  it('exibe badge Receita para tipo RECEITA', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ tipo: 'RECEITA' }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Receita')).toBeTruthy()
    })
  })

  it('exibe traco quando categoria nao tem pai', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ categoriaPaiId: null }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('—')).toBeTruthy()
    })
  })

  it('exibe nome da categoria pai quando categoriaPaiId esta preenchido', async () => {
    const pai = categoriaFixture({ id: 'cat-pai', nome: 'Despesas Fixas', tipo: 'DESPESA' })
    const filha = categoriaFixture({ id: 'cat-001', nome: 'Agua', categoriaPaiId: 'cat-pai' })

    vi.mocked(categoriasService.buscar).mockResolvedValue(filha)
    vi.mocked(categoriasService.listar).mockResolvedValue([pai, filha])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Agua')).toBeTruthy()
    })

    expect(screen.getByText('Despesas Fixas')).toBeTruthy()
  })

  it('exibe badge Nao para categoria nao-sistema', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ system: false }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nao')).toBeTruthy()
    })
  })

  it('exibe badge Sim para categoria do sistema', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ system: true }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Sim')).toBeTruthy()
    })
  })

  it('exibe botao Editar para categoria nao-sistema', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ system: false }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /editar/i })).toBeTruthy()
    })
  })

  it('oculta botao Editar para categoria do sistema', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ system: true }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })

    expect(screen.queryByRole('button', { name: /editar/i })).toBeNull()
  })

  it('navega para /categorias ao clicar em Voltar', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture())
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockPush).toHaveBeenCalledWith('/categorias')
  })

  it('navega para /categorias/id/editar ao clicar em Editar', async () => {
    vi.mocked(categoriasService.buscar).mockResolvedValue(categoriaFixture({ id: 'cat-001', system: false }))
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriaDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /editar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/categorias/cat-001/editar')
  })
})
