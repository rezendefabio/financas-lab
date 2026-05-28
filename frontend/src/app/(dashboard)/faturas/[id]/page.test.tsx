import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/features/fatura', () => ({
  buscarFatura: vi.fn(),
  atualizarFatura: vi.fn(),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'fatura-001' }),
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/faturas/fatura-001',
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EditarFaturaPage from './page'
import type { FaturaResponse } from '@/features/fatura'

const faturaFixture: FaturaResponse = {
  id: 'fatura-001',
  contaId: 'conta-1',
  nome: 'Cartao Maio',
  dataVencimento: '2026-06-10',
  dataFechamento: '2026-06-03',
  valorTotal: { valor: 1500, moeda: 'BRL' },
  paga: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

function mockQueryFatura(fatura: FaturaResponse) {
  vi.mocked(useQuery).mockReturnValue({
    data: fatura,
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

describe('EditarFaturaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.mocked(useQueryClient).mockReturnValue({
      invalidateQueries: vi.fn(),
    } as unknown as ReturnType<typeof useQueryClient>)
    mockMutationIdle()
  })

  it('exibe estado de carregamento enquanto useQuery carrega', () => {
    mockQueryLoading()
    render(<EditarFaturaPage />)
    expect(screen.getByText(/carregando/i)).toBeTruthy()
  })

  it('exibe mensagem de erro quando fatura nao encontrada', () => {
    mockQueryError()
    render(<EditarFaturaPage />)
    expect(screen.getByText(/fatura nao encontrada/i)).toBeTruthy()
  })

  it('carrega dados da fatura via useQuery e popula o formulario', async () => {
    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    expect(screen.getByText('Editar Fatura')).toBeTruthy()
    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe(
        'Cartao Maio',
      )
    })
    expect(
      (screen.getByLabelText(/data de vencimento/i) as HTMLInputElement).value,
    ).toBe('2026-06-10')
    expect(
      (screen.getByLabelText(/data de fechamento/i) as HTMLInputElement).value,
    ).toBe('2026-06-03')
  })

  it('renderiza campos do formulario e botoes', () => {
    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/data de vencimento/i)).toBeTruthy()
    expect(screen.getByLabelText(/data de fechamento/i)).toBeTruthy()
    expect(screen.getByLabelText(/valor total/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('chama mutate com valores atualizados ao submeter', async () => {
    const mockMutate = vi.fn()
    vi.mocked(useMutation).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useMutation>)

    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe(
        'Cartao Maio',
      )
    })

    const nomeInput = screen.getByLabelText(/nome/i)
    await userEvent.clear(nomeInput)
    await userEvent.type(nomeInput, 'Cartao Junho')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({ nome: 'Cartao Junho' }),
      )
    })
  })

  it('exibe texto Salvando... quando mutation esta pendente', () => {
    vi.mocked(useMutation).mockReturnValue({
      mutate: vi.fn(),
      isPending: true,
    } as unknown as ReturnType<typeof useMutation>)

    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    expect(screen.getByText(/salvando\.\.\./i)).toBeTruthy()
  })

  it('navega para /faturas ao clicar em Cancelar', async () => {
    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/faturas')
  })

  it('chama router.back() ao clicar no botao Voltar', async () => {
    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })

  it('permite alterar data de vencimento', async () => {
    const mockMutate = vi.fn()
    vi.mocked(useMutation).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useMutation>)

    mockQueryFatura(faturaFixture)
    render(<EditarFaturaPage />)

    await waitFor(() => {
      expect(
        (screen.getByLabelText(/data de vencimento/i) as HTMLInputElement).value,
      ).toBe('2026-06-10')
    })

    fireEvent.change(screen.getByLabelText(/data de vencimento/i), {
      target: { value: '2026-07-10' },
    })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({ dataVencimento: '2026-07-10' }),
      )
    })
  })
})
