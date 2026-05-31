import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EmprestimosPage from './page'
import type { Emprestimo } from '@/features/emprestimo/types/emprestimo'

vi.mock('@/features/emprestimo/services/emprestimo-service', () => ({
  emprestimoService: {
    listar: vi.fn(),
    remover: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/emprestimos',
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

describe('EmprestimosPage', () => {
  it('renderiza titulo e botao Novo', () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    expect(
      screen.getByRole('heading', { name: /Emprestimos/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Novo/i })).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando a lista chega vazia', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText(/Nenhum emprestimo/i)).toBeInTheDocument()
    })
  })

  it('exibe descricao, tipo, valor formatado e status na linha', async () => {
    vi.mocked(emprestimoService.listar).mockResolvedValue([mockEntity])
    renderWithClient(<EmprestimosPage />)
    await waitFor(() => {
      expect(screen.getByText('Emprestei ao Joao')).toBeInTheDocument()
    })
    expect(screen.getByText('Concedido')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*150,00/)).toBeInTheDocument()
    expect(screen.getByText('30/05/2026')).toBeInTheDocument()
    expect(screen.getByText('Em aberto')).toBeInTheDocument()
  })
})
