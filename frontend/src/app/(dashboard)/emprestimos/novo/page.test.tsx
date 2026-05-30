import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
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

describe('NovoEmprestimoPage', () => {
  it('renderiza titulo e botoes Salvar/Cancelar', () => {
    renderWithClient(<NovoEmprestimoPage />)
    expect(
      screen.getByRole('heading', { name: /Novo emprestimo/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })
})
