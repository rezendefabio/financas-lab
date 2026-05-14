import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import CategoriasPage from './page'
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
  criadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('CategoriasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(categoriasService.listar).mockReturnValue(new Promise(() => {}))

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /categorias/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe cabecalhos de tabela Nome, Tipo e Acoes', async () => {
    vi.mocked(categoriasService.listar).mockResolvedValue([
      categoriaFixture(),
    ])

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nome')).toBeTruthy()
    })
    expect(screen.getByText('Tipo')).toBeTruthy()
    expect(screen.getByText('Acoes')).toBeTruthy()
  })

  it('exibe lista de categorias apos carregamento', async () => {
    vi.mocked(categoriasService.listar).mockResolvedValue([
      categoriaFixture({ nome: 'Alimentacao', tipo: 'DESPESA' }),
      categoriaFixture({ id: 'cat-002', nome: 'Salario', tipo: 'RECEITA' }),
    ])

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeTruthy()
    })

    expect(screen.getByText('Salario')).toBeTruthy()
    expect(screen.getByText('Despesa')).toBeTruthy()
    expect(screen.getByText('Receita')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero categorias', async () => {
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma categoria cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira categoria/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(categoriasService.listar).mockRejectedValue(new Error('Network error'))

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar categorias/i)).toBeTruthy()
    })
  })

  it('navega para /categorias/novo ao clicar em Nova Categoria', async () => {
    vi.mocked(categoriasService.listar).mockResolvedValue([])

    render(<CategoriasPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova categoria/i }))

    expect(mockPush).toHaveBeenCalledWith('/categorias/novo')
  })
})
