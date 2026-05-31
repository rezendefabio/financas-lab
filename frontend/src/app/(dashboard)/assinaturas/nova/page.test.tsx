import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NovaAssinaturaPage from './page'

vi.mock('@/features/assinaturas/services/assinatura-service', () => ({
  assinaturaService: {
    criar: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/assinaturas/nova',
}))

import { assinaturaService } from '@/features/assinaturas/services/assinatura-service'

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('NovaAssinaturaPage', () => {
  it('renderiza titulo e botoes Salvar/Cancelar', () => {
    renderWithClient(<NovaAssinaturaPage />)
    expect(screen.getByRole('heading', { name: /Nova Assinatura/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  it('submit com payload valido chama assinaturaService.criar', async () => {
    vi.mocked(assinaturaService.criar).mockResolvedValue({} as never)
    renderWithClient(<NovaAssinaturaPage />)

    fireEvent.change(screen.getByLabelText(/Nome do servico/i), {
      target: { value: 'Netflix' },
    })
    fireEvent.change(screen.getByLabelText(/Data de renovacao/i), {
      target: { value: '2026-06-15' },
    })
    fireEvent.change(screen.getByLabelText(/Valor mensal/i), {
      target: { value: '29,90' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Salvar/i }))

    await waitFor(() => {
      expect(assinaturaService.criar).toHaveBeenCalledTimes(1)
    })
    expect(assinaturaService.criar).toHaveBeenCalledWith(
      expect.objectContaining({ nome: 'Netflix', moeda: 'BRL', dataRenovacao: '2026-06-15' }),
    )
  })
})
