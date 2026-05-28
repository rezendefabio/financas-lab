import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/grupo', () => ({
  listarGrupos: vi.fn(),
  deletarGrupo: vi.fn(),
  criarGrupo: vi.fn(),
  atualizarGrupo: vi.fn(),
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import GruposPage from './page'
import { listarGrupos, deletarGrupo } from '@/features/grupo'
import type { Grupo } from '@/features/grupo'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const grupoFixture = (overrides?: Partial<Grupo>): Grupo => ({
  id: 'grupo-001',
  userId: 'user-001',
  nome: 'Viagem Europa',
  descricao: 'Gastos da viagem',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('GruposPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe cabecalhos de tabela Nome, Descricao, Ativo e Acoes', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture()])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nome')).toBeTruthy()
    })
    expect(screen.getByText('Descricao')).toBeTruthy()
    expect(screen.getByText('Ativo')).toBeTruthy()
    expect(screen.getByText('Acoes')).toBeTruthy()
  })

  it('exibe lista de grupos apos carregamento', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([
      grupoFixture({ nome: 'Viagem Europa' }),
      grupoFixture({ id: 'grupo-002', nome: 'Casa Nova' }),
    ])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })
    expect(screen.getByText('Casa Nova')).toBeTruthy()
  })

  it('exibe situacao ativo como Sim', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture({ ativo: true })])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Sim')).toBeTruthy()
    })
  })

  it('exibe mensagem vazia quando lista retorna zero grupos', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum grupo cadastrado/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeiro grupo/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarGrupos).mockRejectedValue(new Error('Network error'))

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar grupos/i)).toBeTruthy()
    })
  })

  it('navega para /grupos/nova ao clicar em + Novo grupo', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /\+ novo grupo/i }))

    expect(mockPush).toHaveBeenCalledWith('/grupos/nova')
  })

  it('exibe confirmacao ao clicar Excluir', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture()])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('navega para /grupos/{id} ao clicar Editar', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture({ id: 'grupo-001' })])

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/grupos/grupo-001')
  })

  it('chama deletarGrupo ao confirmar exclusao', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture({ id: 'grupo-001' })])
    vi.mocked(deletarGrupo).mockResolvedValue(undefined)

    render(<GruposPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(deletarGrupo).toHaveBeenCalledWith('grupo-001')
    })
  })
})
