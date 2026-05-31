import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EditarEmprestimoPage from './page'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse } from '@/features/emprestimo'

const pushMock = vi.fn()
const idMock = '22222222-2222-2222-2222-222222222222'
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
  useParams: () => ({ id: idMock }),
  usePathname: () => `/emprestimos/${idMock}/editar`,
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

const mockEmprestimo: EmprestimoResponse = {
  id: idMock,
  descricao: 'Emprestimo Existente',
  nomeTerceiro: 'Maria',
  tipo: 'RECEBIDO',
  valor: { valor: 250, moeda: 'BRL' },
  dataEmprestimo: '2026-03-10',
  quitado: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

describe('EditarEmprestimoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carrega os dados e renderiza o formulario de edicao', async () => {
    vi.mocked(emprestimoService.buscarPorId).mockResolvedValue(mockEmprestimo)
    render(<EditarEmprestimoPage />, { wrapper: makeWrapper() })

    expect(await screen.findByText('Editar Emprestimo')).toBeInTheDocument()
    expect(await screen.findByDisplayValue('Emprestimo Existente')).toBeInTheDocument()
  })

  it('exibe mensagem de erro quando o emprestimo nao existe', async () => {
    vi.mocked(emprestimoService.buscarPorId).mockResolvedValue(undefined)
    render(<EditarEmprestimoPage />, { wrapper: makeWrapper() })

    expect(
      await screen.findByText('Emprestimo nao encontrado.'),
    ).toBeInTheDocument()
  })
})
