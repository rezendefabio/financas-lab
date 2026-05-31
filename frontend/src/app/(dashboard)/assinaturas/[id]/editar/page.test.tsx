import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import EditarAssinaturaPage from './page'

vi.mock('@/features/assinaturas/services/assinatura-service', () => ({
  assinaturaService: {
    buscar: vi.fn(),
    atualizar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useParams: () => ({ id: '00000000-0000-0000-0000-000000000001' }),
  usePathname: () => '/assinaturas/00000000-0000-0000-0000-000000000001/editar',
}))

import { assinaturaService } from '@/features/assinaturas/services/assinatura-service'

const exemplo = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  nome: 'Netflix',
  tipo: 'STREAMING' as const,
  valorMensal: { valor: 29.9, moeda: 'BRL' },
  dataRenovacao: '2026-06-15',
  ativa: true,
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

describe('EditarAssinaturaPage', () => {
  it('exibe skeleton enquanto carrega dados', () => {
    vi.mocked(assinaturaService.buscar).mockImplementation(() => new Promise(() => {}))
    const { container } = renderWithClient(<EditarAssinaturaPage />)
    expect(container.querySelector('.animate-pulse, [data-loading]')).toBeTruthy()
  })

  it('renderiza titulo e form apos carregar dados', async () => {
    vi.mocked(assinaturaService.buscar).mockResolvedValue(exemplo)
    renderWithClient(<EditarAssinaturaPage />)
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Editar Assinatura/i })).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('Netflix')).toBeInTheDocument()
  })

  it('submit chama assinaturaService.atualizar', async () => {
    vi.mocked(assinaturaService.buscar).mockResolvedValue(exemplo)
    vi.mocked(assinaturaService.atualizar).mockResolvedValue(exemplo)
    renderWithClient(<EditarAssinaturaPage />)
    await waitFor(() => {
      expect(screen.getByDisplayValue('Netflix')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /Salvar alteracoes/i }))
    await waitFor(() => {
      expect(assinaturaService.atualizar).toHaveBeenCalledTimes(1)
    })
    expect(assinaturaService.atualizar).toHaveBeenCalledWith(
      exemplo.id,
      expect.objectContaining({ nome: 'Netflix', ativa: true }),
    )
  })
})
