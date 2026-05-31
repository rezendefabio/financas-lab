import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push }),
  usePathname: () => '/emprestimos/novo',
}))

vi.mock('@/features/emprestimo', async () => {
  const actual = await vi.importActual<typeof import('@/features/emprestimo')>(
    '@/features/emprestimo',
  )
  return {
    ...actual,
    emprestimoService: {
      criar: vi.fn(),
    },
  }
})

import NovoEmprestimoPage from './page'
import { emprestimoService } from '@/features/emprestimo'

function wrapper({ children }: { children: ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

beforeEach(() => {
  push.mockClear()
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('NovoEmprestimoPage', () => {
  it('renderiza o formulario de criacao', () => {
    render(<NovoEmprestimoPage />, { wrapper })
    expect(screen.getByText('Novo Emprestimo')).toBeInTheDocument()
    expect(screen.getByLabelText('Descricao')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeInTheDocument()
  })

  it('cancela navegando de volta para a listagem', () => {
    render(<NovoEmprestimoPage />, { wrapper })
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(push).toHaveBeenCalledWith('/emprestimos')
  })

  it('submete e cria emprestimo com payload correto', async () => {
    vi.mocked(emprestimoService.criar).mockResolvedValue({
      id: 'x',
      descricao: 'Compra',
      nomeTerceiro: null,
      tipo: 'CONCEDIDO',
      valor: { valor: 300, moeda: 'BRL' },
      dataEmprestimo: '2026-03-10',
      quitado: false,
      criadoEm: '2026-01-01T00:00:00Z',
      atualizadoEm: '2026-01-01T00:00:00Z',
    })
    render(<NovoEmprestimoPage />, { wrapper })

    fireEvent.change(screen.getByLabelText('Descricao'), {
      target: { value: 'Compra' },
    })
    fireEvent.change(screen.getByLabelText('Data do emprestimo'), {
      target: { value: '2026-03-10' },
    })
    fireEvent.change(screen.getByLabelText('Valor (R$)'), {
      target: { value: 'R$ 300,00' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(emprestimoService.criar).toHaveBeenCalledWith(
        expect.objectContaining({
          descricao: 'Compra',
          tipo: 'CONCEDIDO',
          valor: 300,
          moeda: 'BRL',
          dataEmprestimo: '2026-03-10',
          quitado: false,
        }),
      ),
    )
    await waitFor(() => expect(push).toHaveBeenCalledWith('/emprestimos'))
  })
})
