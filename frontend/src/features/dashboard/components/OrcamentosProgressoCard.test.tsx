import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/orcamentos/services/orcamento-service', () => ({
  orcamentoService: {
    listar: vi.fn(),
    progresso: vi.fn(),
  },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

import { OrcamentosProgressoCard } from './OrcamentosProgressoCard'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import type { Orcamento, Progresso } from '@/features/orcamentos/types/orcamento'
import type { Categoria } from '@/features/categorias/types/categoria'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

function mesAtualPrefixo(): string {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`
}

const categoriaFixture = (overrides?: Partial<Categoria>): Categoria => ({
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  system: false,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

const orcamentoFixture = (overrides?: Partial<Orcamento>): Orcamento => ({
  id: 'orc-001',
  categoriaId: 'cat-001',
  valorLimite: { valor: 500, moeda: 'BRL' },
  mesAno: `${mesAtualPrefixo()}-01`,
  ativo: true,
  criadoEm: '2024-05-01T00:00:00Z',
  atualizadoEm: '2024-05-01T00:00:00Z',
  ...overrides,
})

const progressoFixture = (overrides?: Partial<Progresso>): Progresso => ({
  orcamentoId: 'orc-001',
  categoriaId: 'cat-001',
  mesAno: `${mesAtualPrefixo()}-01`,
  valorLimite: { valor: 500, moeda: 'BRL' },
  totalGasto: { valor: 100, moeda: 'BRL' },
  percentualUtilizado: 20,
  status: 'ABAIXO',
  ...overrides,
})

describe('OrcamentosProgressoCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(categoriasService.listar).mockResolvedValue([categoriaFixture()])
  })

  it('exibe titulo Orcamentos do Mes', () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([])
    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })
    expect(screen.getByText(/Orcamentos do Mes/i)).toBeInTheDocument()
  })

  it('exibe mensagem quando nao ha orcamentos no mes atual', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([])
    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(
        screen.getByText(/Nenhum orcamento cadastrado para este mes/i),
      ).toBeInTheDocument()
    })
  })

  it('filtra orcamentos de outros meses', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ mesAno: '2020-01-01' }),
    ])
    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(
        screen.getByText(/Nenhum orcamento cadastrado para este mes/i),
      ).toBeInTheDocument()
    })
  })

  it('filtra orcamentos inativos', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ ativo: false }),
    ])
    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(
        screen.getByText(/Nenhum orcamento cadastrado para este mes/i),
      ).toBeInTheDocument()
    })
  })

  it('exibe barra verde para orcamento status ABAIXO', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())

    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeInTheDocument()
    })

    const progressbar = screen.getByRole('progressbar')
    const barra = progressbar.querySelector('div')
    expect(barra?.className).toContain('bg-green-500')
    expect(screen.getByText(/20%/)).toBeInTheDocument()
  })

  it('exibe barra vermelha para orcamento status EXCEDIDO e limita largura a 100%', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({
        status: 'EXCEDIDO',
        percentualUtilizado: 150,
        totalGasto: { valor: 750, moeda: 'BRL' },
      }),
    )

    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Alimentacao')).toBeInTheDocument()
    })

    const progressbar = screen.getByRole('progressbar')
    const barra = progressbar.querySelector('div') as HTMLElement
    expect(barra.className).toContain('bg-red-500')
    expect(barra.style.width).toBe('100%')
    expect(screen.getByText(/150%/)).toBeInTheDocument()
  })

  it('exibe valores totalGasto e valorLimite formatados em BRL', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())

    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/100,00/)).toBeInTheDocument()
    })
    expect(screen.getByText(/500,00/)).toBeInTheDocument()
  })

  it('exibe fallback Categoria quando categoria nao encontrada', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ categoriaId: 'cat-inexistente' }),
    ])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())

    render(<OrcamentosProgressoCard />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Categoria')).toBeInTheDocument()
    })
  })
})
