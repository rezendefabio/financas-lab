import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/features/metas/services/meta-service', () => ({
  metaService: {
    buscar: vi.fn(),
    cancelar: vi.fn(),
    registrarDeposito: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'meta-001' }),
  useRouter: () => ({ push: mockPush }),
  usePathname: () => '/metas/meta-001',
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import MetaDetalhePage from './page'
import type { Meta } from '@/features/metas/types/meta'

const metaEmAndamento: Meta = {
  id: 'meta-001',
  nome: 'Viagem Europa',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 3000, moeda: 'BRL' },
  prazo: '2027-12-31',
  status: 'EM_ANDAMENTO',
  atrasada: false,
  percentualConcluido: 30,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

const metaConcluida: Meta = {
  ...metaEmAndamento,
  status: 'CONCLUIDA',
  percentualConcluido: 100,
  valorAtual: { valor: 10000, moeda: 'BRL' },
}

const metaAtrasada: Meta = {
  ...metaEmAndamento,
  atrasada: true,
}

function mockQueryMeta(meta: Meta) {
  vi.mocked(useQuery).mockReturnValue({
    data: meta,
    isLoading: false,
    isError: false,
  } as ReturnType<typeof useQuery>)
}

function mockQueryLoading() {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: true,
    isError: false,
  } as ReturnType<typeof useQuery>)
}

function mockQueryError() {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: true,
  } as ReturnType<typeof useQuery>)
}

function mockMutationIdle() {
  vi.mocked(useMutation).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useMutation>)
}

describe('MetaDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useQueryClient).mockReturnValue({
      invalidateQueries: vi.fn(),
    } as unknown as ReturnType<typeof useQueryClient>)
    mockMutationIdle()
  })

  describe('estado de loading', () => {
    it('exibe skeleton enquanto carrega', () => {
      mockQueryLoading()
      render(<MetaDetalhePage />)
      const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
      expect(skeletons.length).toBeGreaterThan(0)
    })
  })

  describe('estado de erro', () => {
    it('exibe mensagem de erro quando meta nao encontrada', () => {
      mockQueryError()
      render(<MetaDetalhePage />)
      expect(screen.getByText(/erro ao carregar meta/i)).toBeTruthy()
    })
  })

  describe('happy path — meta em andamento', () => {
    it('exibe nome da meta', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      expect(screen.getByText('Viagem Europa')).toBeTruthy()
    })

    it('exibe badge Em Andamento', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      expect(screen.getByText('Em Andamento')).toBeTruthy()
    })

    it('exibe valor alvo formatado em BRL', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      // formatBRL pode usar espaco nao-separavel (U+00A0) em JSDOM
      const allText = document.body.textContent ?? ''
      expect(allText).toMatch(/10[\s\S]000/)
    })

    it('exibe percentual concluido', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      // toFixed(1) retorna ponto decimal em JS (30.0), nao virgula
      expect(screen.getByText(/30\.0% concluido/i)).toBeTruthy()
    })

    it('exibe card de Registrar Deposito para meta em andamento', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      expect(screen.getByText('Registrar Deposito')).toBeTruthy()
    })

    it('exibe botao Cancelar Meta para meta em andamento', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      expect(screen.getByRole('button', { name: /cancelar meta/i })).toBeTruthy()
    })

    it('exibe badge Atrasada quando meta esta atrasada', () => {
      mockQueryMeta(metaAtrasada)
      render(<MetaDetalhePage />)
      expect(screen.getByText('Atrasada')).toBeTruthy()
    })

    it('navega para /metas ao clicar em Voltar', async () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /voltar/i }))
      expect(mockPush).toHaveBeenCalledWith('/metas')
    })
  })

  describe('happy path — meta concluida', () => {
    it('exibe badge Concluida', () => {
      mockQueryMeta(metaConcluida)
      render(<MetaDetalhePage />)
      expect(screen.getByText('Concluida')).toBeTruthy()
    })

    it('nao exibe card de deposito para meta concluida', () => {
      mockQueryMeta(metaConcluida)
      render(<MetaDetalhePage />)
      expect(screen.queryByText('Registrar Deposito')).toBeNull()
    })

    it('nao exibe botao Cancelar Meta para meta concluida', () => {
      mockQueryMeta(metaConcluida)
      render(<MetaDetalhePage />)
      expect(screen.queryByRole('button', { name: /cancelar meta/i })).toBeNull()
    })
  })

  describe('fluxo de deposito', () => {
    it('exibe campo de valor no formulario de deposito', () => {
      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)
      expect(screen.getByLabelText(/valor/i)).toBeTruthy()
      expect(screen.getByRole('button', { name: /registrar/i })).toBeTruthy()
    })

    it('chama mutate ao submeter formulario de deposito', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)

      const valorInput = screen.getByLabelText(/valor/i)
      await userEvent.clear(valorInput)
      await userEvent.type(valorInput, '500')

      await userEvent.click(screen.getByRole('button', { name: /registrar/i }))

      await waitFor(() => {
        expect(mockMutate).toHaveBeenCalled()
      })
    })
  })

  describe('fluxo de cancelamento', () => {
    it('chama mutate ao clicar em Cancelar Meta', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)

      await userEvent.click(screen.getByRole('button', { name: /cancelar meta/i }))

      expect(mockMutate).toHaveBeenCalled()
    })

    it('exibe texto Cancelando... quando mutation esta pendente', () => {
      vi.mocked(useMutation).mockReturnValue({
        mutate: vi.fn(),
        isPending: true,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryMeta(metaEmAndamento)
      render(<MetaDetalhePage />)

      expect(screen.getByText(/cancelando\.\.\./i)).toBeTruthy()
    })
  })
})
