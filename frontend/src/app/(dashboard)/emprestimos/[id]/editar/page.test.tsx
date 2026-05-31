import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'

const push = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push }),
  usePathname: () => '/emprestimos/abc-1/editar',
  useParams: () => ({ id: 'abc-1' }),
}))

vi.mock('@/features/emprestimo', async () => {
  const actual = await vi.importActual<typeof import('@/features/emprestimo')>(
    '@/features/emprestimo',
  )
  return {
    ...actual,
    emprestimoService: {
      buscarPorId: vi.fn(),
      atualizar: vi.fn(),
    },
  }
})

import EditarEmprestimoPage from './page'
import { emprestimoService } from '@/features/emprestimo'
import type { EmprestimoResponse } from '@/features/emprestimo'

const item: EmprestimoResponse = {
  id: 'abc-1',
  descricao: 'Antiga',
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
  vi.mocked(emprestimoService.buscarPorId).mockResolvedValue(item)
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('EditarEmprestimoPage', () => {
  it('carrega os dados e preenche o formulario', async () => {
    render(<EditarEmprestimoPage />, { wrapper })
    await waitFor(() =>
      expect(screen.getByLabelText('Descricao')).toHaveValue('Antiga'),
    )
  })

  it('atualiza com os campos editados', async () => {
    vi.mocked(emprestimoService.atualizar).mockResolvedValue({
      ...item,
      descricao: 'Nova',
      quitado: true,
    })
    render(<EditarEmprestimoPage />, { wrapper })
    await waitFor(() =>
      expect(screen.getByLabelText('Descricao')).toHaveValue('Antiga'),
    )

    fireEvent.change(screen.getByLabelText('Descricao'), {
      target: { value: 'Nova' },
    })
    fireEvent.click(screen.getByLabelText('Quitado'))
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() =>
      expect(emprestimoService.atualizar).toHaveBeenCalledWith(
        'abc-1',
        expect.objectContaining({ descricao: 'Nova', quitado: true }),
      ),
    )
    await waitFor(() => expect(push).toHaveBeenCalledWith('/emprestimos'))
  })
})
