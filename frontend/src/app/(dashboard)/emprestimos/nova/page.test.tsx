import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NovoEmprestimoPage from './page'
import { emprestimoService } from '@/features/emprestimo'

const pushMock = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
  usePathname: () => '/emprestimos/nova',
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

describe('NovoEmprestimoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza o formulario de novo emprestimo', () => {
    render(<NovoEmprestimoPage />, { wrapper: makeWrapper() })
    expect(screen.getByText('Novo Emprestimo')).toBeInTheDocument()
    expect(screen.getByLabelText('Descricao')).toBeInTheDocument()
    expect(screen.getByText('Salvar')).toBeInTheDocument()
  })
})
