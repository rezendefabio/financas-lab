import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AssinaturasPage from './page'

vi.mock('@/features/assinaturas/services/assinatura-service', () => ({
  assinaturaService: {
    listar: vi.fn(),
    remover: vi.fn(),
  },
}))
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/assinaturas',
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

describe('AssinaturasPage', () => {
  it('renderiza titulo e botao Nova', async () => {
    vi.mocked(assinaturaService.listar).mockResolvedValue([])
    renderWithClient(<AssinaturasPage />)
    expect(screen.getByRole('heading', { name: /Assinaturas/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Nova/i })).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando a lista chega vazia', async () => {
    vi.mocked(assinaturaService.listar).mockResolvedValue([])
    renderWithClient(<AssinaturasPage />)
    await waitFor(() => {
      expect(screen.getByText(/Nenhuma assinatura/i)).toBeInTheDocument()
    })
  })

  it('exibe linhas com nome, tipo e valor formatado', async () => {
    vi.mocked(assinaturaService.listar).mockResolvedValue([exemplo])
    renderWithClient(<AssinaturasPage />)
    await waitFor(() => {
      expect(screen.getByText('Netflix')).toBeInTheDocument()
    })
    expect(screen.getByText('Streaming')).toBeInTheDocument()
    expect(screen.getByText(/29,90/)).toBeInTheDocument()
  })

  it('clicar em Excluir abre confirmacao e confirma chama remover', async () => {
    vi.mocked(assinaturaService.listar).mockResolvedValue([exemplo])
    vi.mocked(assinaturaService.remover).mockResolvedValue(undefined)
    renderWithClient(<AssinaturasPage />)
    await waitFor(() => {
      expect(screen.getByText('Netflix')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: /Excluir/i }))
    const confirmar = await screen.findByRole('button', { name: /Confirmar/i })
    fireEvent.click(confirmar)
    await waitFor(() => {
      expect(assinaturaService.remover).toHaveBeenCalledWith(exemplo.id)
    })
  })
})
