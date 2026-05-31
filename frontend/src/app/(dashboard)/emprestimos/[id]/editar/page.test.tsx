import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EditarEmprestimoPage from './page'
import type { Emprestimo } from '@/features/emprestimo/types/emprestimo'

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

const mockEntity: Emprestimo = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  descricao: 'Emprestei ao Joao',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO',
  valor: { valor: 150, moeda: 'BRL' },
  dataEmprestimo: '2026-05-30',
  quitado: false,
  criadoEm: '2026-05-30T00:00:00Z',
  atualizadoEm: '2026-05-30T00:00:00Z',
}

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
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
    expect(container.querySelector('.animate-pulse, [data-loading]')).toBeTruthy()
  })

  it('renderiza titulo e form preenchido apos carregar dados', async () => {
    vi.mocked(emprestimoService.buscar).mockResolvedValue(mockEntity)
    renderWithClient(<EditarEmprestimoPage />)
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Editar Emprestimo/i }),
      ).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('Emprestei ao Joao')).toBeInTheDocument()
  })

  it('submit chama emprestimoService.atualizar', async () => {
    vi.mocked(emprestimoService.buscar).mockResolvedValue(mockEntity)
    vi.mocked(emprestimoService.atualizar).mockResolvedValue(mockEntity)
    const { default: userEvent } = await import('@testing-library/user-event')
    const user = userEvent.setup()
    renderWithClient(<EditarEmprestimoPage />)

    await waitFor(() => {
      expect(screen.getByDisplayValue('Emprestei ao Joao')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Salvar alteracoes/i }))

    await waitFor(() => {
      expect(emprestimoService.atualizar).toHaveBeenCalledTimes(1)
    })
    expect(vi.mocked(emprestimoService.atualizar).mock.calls[0][0]).toBe(
      '00000000-0000-0000-0000-000000000001',
    )
  })
})
