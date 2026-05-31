import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push }),
  usePathname: () => '/emprestimos',
}))

vi.mock('@/features/emprestimo', async () => {
  const actual = await vi.importActual<typeof import('@/features/emprestimo')>(
    '@/features/emprestimo',
  )
  return {
    ...actual,
    emprestimoService: {
      listar: vi.fn(),
      deletar: vi.fn(),
    },
  }
})

import EmprestimosPage from './page'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse } from '@/features/emprestimo'

const item: EmprestimoResponse = {
  id: 'abc-1',
  descricao: 'Emprestimo ao Joao',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO',
  valor: { valor: 500, moeda: 'BRL' },
  dataEmprestimo: '2026-01-15',
  quitado: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

function wrapper({ children }: { children: ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>
}

beforeEach(() => {
  push.mockClear()
  vi.mocked(emprestimoService.listar).mockResolvedValue([item])
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('EmprestimosPage', () => {
  it('renderiza a listagem com descricao, tipo e status', async () => {
    render(<EmprestimosPage />, { wrapper })
    expect(await screen.findByText('Emprestimo ao Joao')).toBeInTheDocument()
    expect(screen.getByText('Concedido')).toBeInTheDocument()
    expect(screen.getByText('Em aberto')).toBeInTheDocument()
  })

  it('navega para novo ao clicar em Novo Emprestimo', async () => {
    render(<EmprestimosPage />, { wrapper })
    await screen.findByText('Emprestimo ao Joao')
    fireEvent.click(screen.getByRole('button', { name: 'Novo Emprestimo' }))
    expect(push).toHaveBeenCalledWith('/emprestimos/novo')
  })

  it('navega para editar ao clicar em Editar', async () => {
    render(<EmprestimosPage />, { wrapper })
    await screen.findByText('Emprestimo ao Joao')
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }))
    expect(push).toHaveBeenCalledWith('/emprestimos/abc-1/editar')
  })

  it('confirma exclusao em duas etapas', async () => {
    vi.mocked(emprestimoService.deletar).mockResolvedValue(undefined)
    render(<EmprestimosPage />, { wrapper })
    await screen.findByText('Emprestimo ao Joao')
    fireEvent.click(screen.getByRole('button', { name: 'Excluir' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    await waitFor(() =>
      expect(emprestimoService.deletar).toHaveBeenCalledWith('abc-1'),
    )
  })
})
