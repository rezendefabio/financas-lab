import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/tag', () => ({
  listarTags: vi.fn(),
  deletarTag: vi.fn(),
  criarTag: vi.fn(),
  atualizarTag: vi.fn(),
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import TagsPage from './page'
import { listarTags, deletarTag } from '@/features/tag'
import type { Tag } from '@/features/tag'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const tagFixture = (overrides?: Partial<Tag>): Tag => ({
  id: 'tag-001',
  userId: 'user-001',
  nome: 'Urgente',
  cor: '#FF5733',
  criadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('TagsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(listarTags).mockReturnValue(new Promise(() => {}))

    render(<TagsPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /tags/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe cabecalhos de tabela Nome, Cor e Acoes', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nome')).toBeTruthy()
    })
    expect(screen.getByText('Cor')).toBeTruthy()
    expect(screen.getByText('Acoes')).toBeTruthy()
  })

  it('exibe lista de tags apos carregamento', async () => {
    vi.mocked(listarTags).mockResolvedValue([
      tagFixture({ nome: 'Urgente', cor: '#FF5733' }),
      tagFixture({ id: 'tag-002', nome: 'Importante', cor: '#00FF00' }),
    ])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })
    expect(screen.getByText('Importante')).toBeTruthy()
  })

  it('exibe preview de cor como circulo colorido', async () => {
    vi.mocked(listarTags).mockResolvedValue([
      tagFixture({ nome: 'Urgente', cor: '#FF5733' }),
    ])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })
    const colorCircle = screen.getByLabelText('Cor: #FF5733')
    expect(colorCircle).toBeTruthy()
    expect(colorCircle.style.backgroundColor).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero tags', async () => {
    vi.mocked(listarTags).mockResolvedValue([])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma tag cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira tag/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarTags).mockRejectedValue(new Error('Network error'))

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar tags/i)).toBeTruthy()
    })
  })

  it('navega para /tags/novo ao clicar em + Nova Tag', async () => {
    vi.mocked(listarTags).mockResolvedValue([])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /\+ nova tag/i }))

    expect(mockPush).toHaveBeenCalledWith('/tags/novo')
  })

  it('exibe confirmacao ao clicar Excluir', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('cancela exclusao ao clicar Cancelar', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(screen.getByRole('button', { name: /excluir/i })).toBeTruthy()
  })

  it('navega para /tags/{id}/editar ao clicar Editar', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture({ id: 'tag-001' })])

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/tags/tag-001/editar')
  })

  it('chama deletarTag e invalida cache ao confirmar exclusao', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture({ id: 'tag-001' })])
    vi.mocked(deletarTag).mockResolvedValue(undefined)

    render(<TagsPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Urgente')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(deletarTag).toHaveBeenCalledWith('tag-001')
    })
  })
})
