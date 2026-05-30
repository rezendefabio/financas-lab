import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EmprestimosPage from './page'

vi.mock('@/features/emprestimos/services/emprestimos-service', () => ({
  emprestimosService: {
    listar: vi.fn(),
    remover: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

import { emprestimosService } from '@/features/emprestimos/services/emprestimos-service'

const mockEmprestimo = {
  id: '00000000-0000-0000-0000-000000000001',
  descricao: 'Emprestimo ao Joao',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO' as const,
  valor: { valor: 100, moeda: 'BRL' },
  dataEmprestimo: '2026-05-30',
  quitado: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EmprestimosPage', () => {
  it('renderiza titulo e botao Novo', async () => {
    vi.mocked(emprestimosService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    expect(
      screen.getByRole('heading', { name: /Emprestimos/i }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /Novo Emprestimo/i }),
    ).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando a lista chega vazia', async () => {
    vi.mocked(emprestimosService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText(/Nenhum emprestimo/i)).toBeInTheDocument()
    })
  })

  it('exibe linhas com descricao, valor formatado e status', async () => {
    vi.mocked(emprestimosService.listar).mockResolvedValue([mockEmprestimo])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText('Emprestimo ao Joao')).toBeInTheDocument()
    })
    expect(screen.getByText(/R\$\s?100,00/)).toBeInTheDocument()
    expect(screen.getByText('Em aberto')).toBeInTheDocument()
    expect(screen.getByText('Concedido')).toBeInTheDocument()
  })

  it('clicar em Excluir abre confirmacao e confirmar chama remover', async () => {
    const user = userEvent.setup()
    vi.mocked(emprestimosService.listar).mockResolvedValue([mockEmprestimo])
    vi.mocked(emprestimosService.remover).mockResolvedValue(undefined)
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText('Emprestimo ao Joao')).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /Excluir/i }))
    const confirmar = await screen.findByRole('button', { name: /Confirmar/i })
    await user.click(confirmar)
    await waitFor(() => {
      expect(emprestimosService.remover).toHaveBeenCalledWith(mockEmprestimo.id)
    })
  })
})
