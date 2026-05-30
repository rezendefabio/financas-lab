import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EmprestimosPage from './page'

vi.mock('@/features/emprestimo/services/emprestimo-service', () => ({
  emprestimoService: {
    listar: vi.fn(),
    remover: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
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

describe('EmprestimosPage', () => {
  it('renderiza titulo e botao Novo', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    expect(
      screen.getByRole('heading', { name: /emprestimos/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /\+ novo/i })).toBeInTheDocument()
  })

  it('renderiza lista quando o service retorna itens', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([
      {
        id: 'a',
        descricao: 'Emp Joao',
        nomeTerceiro: 'Joao',
        tipo: 'CONCEDIDO',
        valor: { valor: 100, moeda: 'BRL' },
        dataEmprestimo: '2026-01-15',
        quitado: false,
        criadoEm: '2026-01-15T00:00:00Z',
        atualizadoEm: '2026-01-15T00:00:00Z',
      },
    ])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText('Emp Joao')).toBeInTheDocument()
    })
    expect(screen.getByText('Em aberto')).toBeInTheDocument()
  })

  it('exibe mensagem vazia quando nao ha itens', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(
        screen.getByText(/nenhum emprestimo cadastrado/i),
      ).toBeInTheDocument()
    })
  })
})
