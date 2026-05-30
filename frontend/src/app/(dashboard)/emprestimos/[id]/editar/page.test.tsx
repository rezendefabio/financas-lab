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
  useParams: () => ({ id: '00000000-0000-0000-0000-000000000001' }),
  usePathname: () => '/emprestimos/00000000-0000-0000-0000-000000000001/editar',
}))

import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EditarEmprestimoPage', () => {
  it('exibe skeleton enquanto carrega dados', () => {
    vi.mocked(emprestimoService.buscar).mockImplementation(
      () => new Promise(() => {}),
    )
    const { container } = renderWithClient(<EditarEmprestimoPage />)
    expect(
      container.querySelector('.animate-pulse, [data-loading]'),
    ).toBeTruthy()
  })

  it('renderiza titulo e form apos carregar dados', async () => {
    vi.mocked(emprestimoService.buscar).mockResolvedValue({
      id: '00000000-0000-0000-0000-000000000001',
      userId: '00000000-0000-0000-0000-000000000099',
      descricao: 'Existente',
      nomeTerceiro: 'Maria',
      tipo: 'RECEBIDO',
      valor: { valor: 99.9, moeda: 'BRL' },
      dataEmprestimo: '2026-02-01',
      quitado: false,
      criadoEm: '2026-02-01T00:00:00Z',
      atualizadoEm: '2026-02-01T00:00:00Z',
    })
    renderWithClient(<EditarEmprestimoPage />)
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Editar emprestimo/i }),
      ).toBeInTheDocument()
    })
  })
})
