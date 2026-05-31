import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EmprestimosPage from './page'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse } from '@/features/emprestimo'

const pushMock = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
  usePathname: () => '/emprestimos',
}))

vi.mock('@/features/emprestimo', async () => {
  const actual =
    await vi.importActual<typeof import('@/features/emprestimo')>(
      '@/features/emprestimo',
    )
  return {
    ...actual,
    emprestimoService: {
      listar: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      deletar: vi.fn(),
      buscarPorId: vi.fn(),
    },
  }
})

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const mockEmprestimos: EmprestimoResponse[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    descricao: 'Emprestimo ao Joao',
    nomeTerceiro: 'Joao',
    tipo: 'CONCEDIDO',
    valor: { valor: 100, moeda: 'BRL' },
    dataEmprestimo: '2026-01-15',
    quitado: false,
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
  },
]

describe('EmprestimosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza a listagem de emprestimos', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue(mockEmprestimos)
    render(<EmprestimosPage />, { wrapper: makeWrapper() })

    expect(await screen.findByText('Emprestimo ao Joao')).toBeInTheDocument()
  })

  it('exibe estado vazio quando nao ha emprestimos', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([])
    render(<EmprestimosPage />, { wrapper: makeWrapper() })

    expect(
      await screen.findByText('Nenhum emprestimo encontrado.'),
    ).toBeInTheDocument()
  })
})
