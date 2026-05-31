import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NovoEmprestimoPage from './page'

vi.mock('@/features/emprestimo/services/emprestimo-service', () => ({
  emprestimoService: {
    criar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/emprestimos/novo',
}))

import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'

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

  it('submit com payload valido chama emprestimoService.criar', async () => {
    vi.mocked(emprestimoService.criar).mockResolvedValue({} as never)
    const { default: userEvent } = await import('@testing-library/user-event')
    const user = userEvent.setup()
    renderWithClient(<NovoEmprestimoPage />)

    await user.type(screen.getByLabelText('Descricao'), 'Emprestei ao Joao')
    await user.type(screen.getByLabelText('Data do emprestimo'), '2026-05-30')
    await user.type(screen.getByLabelText('Valor (R$)'), '150')

    await user.click(screen.getByRole('button', { name: /Salvar/i }))

    await waitFor(() => {
      expect(emprestimoService.criar).toHaveBeenCalledTimes(1)
    })
    const arg = vi.mocked(emprestimoService.criar).mock.calls[0][0]
    expect(arg.descricao).toBe('Emprestei ao Joao')
    expect(arg.tipo).toBe('CONCEDIDO')
    expect(arg.moeda).toBe('BRL')
    expect(arg.valor).toBe(150)
  })
})
