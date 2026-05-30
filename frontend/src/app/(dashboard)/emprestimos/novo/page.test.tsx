import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NovoEmprestimoPage from './page'

vi.mock('@/features/emprestimos/services/emprestimos-service', () => ({
  emprestimosService: {
    criar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/emprestimos/novo',
}))

import { emprestimosService } from '@/features/emprestimos/services/emprestimos-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('NovoEmprestimoPage', () => {
  it('renderiza titulo e botoes Salvar/Cancelar', () => {
    renderWithClient(<NovoEmprestimoPage />)
    expect(
      screen.getByRole('heading', { name: /Novo Emprestimo/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  it('submete payload valido e chama criar', async () => {
    const user = userEvent.setup()
    vi.mocked(emprestimosService.criar).mockResolvedValue({} as never)
    renderWithClient(<NovoEmprestimoPage />)

    await user.type(screen.getByLabelText(/Descricao/i), 'Emprestimo ao Joao')
    await user.type(screen.getByLabelText(/Valor/i), '100')
    const dataInput = document.querySelector('input[type="date"]') as HTMLInputElement
    await user.type(dataInput, '2026-05-30')

    await user.click(screen.getByRole('button', { name: /Salvar/i }))

    await waitFor(() => {
      expect(emprestimosService.criar).toHaveBeenCalledTimes(1)
    })
    expect(vi.mocked(emprestimosService.criar).mock.calls[0][0]).toMatchObject({
      descricao: 'Emprestimo ao Joao',
      tipo: 'CONCEDIDO',
      moeda: 'BRL',
      dataEmprestimo: '2026-05-30',
    })
  })
})
