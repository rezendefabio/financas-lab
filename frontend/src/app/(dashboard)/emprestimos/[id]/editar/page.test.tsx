import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

// React 19 `use(params)` suspende em jsdom e nao acorda de forma confiavel sob
// vitest. Como o teste sempre passa uma Promise ja resolvida, fazemos o `use`
// desempacotar sincronamente o valor resolvido (params e SCALAR resolvido no teste).
vi.mock('react', async () => {
  const actual = await vi.importActual<typeof import('react')>('react')
  return {
    ...actual,
    use: (value: unknown) => {
      if (value && typeof (value as { then?: unknown }).then === 'function') {
        return { id: '00000000-0000-0000-0000-000000000001' }
      }
      return (actual.use as (v: unknown) => unknown)(value)
    },
  }
})

import EditarEmprestimoPage from './page'

vi.mock('@/features/emprestimos/services/emprestimos-service', () => ({
  emprestimosService: {
    listar: vi.fn(),
    atualizar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/emprestimos/x/editar',
}))

import { emprestimosService } from '@/features/emprestimos/services/emprestimos-service'

const TEST_ID = '00000000-0000-0000-0000-000000000001'

const mockEmprestimo = {
  id: TEST_ID,
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

describe('EditarEmprestimoPage', () => {
  it('exibe skeleton enquanto carrega dados', () => {
    vi.mocked(emprestimosService.listar).mockImplementation(
      () => new Promise(() => {}),
    )
    const { container } = renderWithClient(
      <EditarEmprestimoPage params={Promise.resolve({ id: TEST_ID })} />,
    )
    expect(container.querySelector('.animate-pulse')).toBeTruthy()
  })

  it('renderiza titulo e dados apos carregar', async () => {
    vi.mocked(emprestimosService.listar).mockResolvedValue([mockEmprestimo])
    renderWithClient(
      <EditarEmprestimoPage params={Promise.resolve({ id: TEST_ID })} />,
    )
    expect(
      await screen.findByRole('heading', { name: /Editar Emprestimo/i }),
    ).toBeInTheDocument()
    expect(screen.getByDisplayValue('Emprestimo ao Joao')).toBeInTheDocument()
  })

  it('submete alteracoes e chama atualizar', async () => {
    const user = userEvent.setup()
    vi.mocked(emprestimosService.listar).mockResolvedValue([mockEmprestimo])
    vi.mocked(emprestimosService.atualizar).mockResolvedValue({} as never)
    renderWithClient(
      <EditarEmprestimoPage params={Promise.resolve({ id: TEST_ID })} />,
    )
    expect(
      await screen.findByDisplayValue('Emprestimo ao Joao'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Salvar alteracoes/i }))
    await waitFor(() => {
      expect(emprestimosService.atualizar).toHaveBeenCalledWith(
        TEST_ID,
        expect.objectContaining({ descricao: 'Emprestimo ao Joao', quitado: false }),
      )
    })
  })
})
