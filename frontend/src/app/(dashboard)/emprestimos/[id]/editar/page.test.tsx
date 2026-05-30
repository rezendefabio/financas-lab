import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EditarEmprestimoPage from './page'

vi.mock('@/features/emprestimo/services/emprestimo-service', () => ({
  emprestimoService: {
    buscar: vi.fn(),
    atualizar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useParams: () => ({ id: 'abc-123' }),
  usePathname: () => '/emprestimos/abc-123/editar',
}))

import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EditarEmprestimoPage', () => {
  it('exibe formulario preenchido apos carregar dados', async () => {
    vi.mocked(emprestimoService.buscar).mockResolvedValue({
      id: 'abc-123',
      descricao: 'Emp Joao',
      nomeTerceiro: 'Joao',
      tipo: 'RECEBIDO',
      valor: { valor: 250.5, moeda: 'BRL' },
      dataEmprestimo: '2026-01-15',
      quitado: true,
      criadoEm: '2026-01-15T00:00:00Z',
      atualizadoEm: '2026-01-15T00:00:00Z',
    })
    renderWithClient(<EditarEmprestimoPage />)
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /editar emprestimo/i }),
      ).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('Emp Joao')).toBeInTheDocument()
  })

  it('exibe mensagem de erro se busca falha', async () => {
    vi.mocked(emprestimoService.buscar).mockRejectedValue(new Error('boom'))
    renderWithClient(<EditarEmprestimoPage />)
    await waitFor(() => {
      expect(
        screen.getByText(/erro ao carregar emprestimo/i),
      ).toBeInTheDocument()
    })
  })
})
