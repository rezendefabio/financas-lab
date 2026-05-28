import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/features/carteira', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/carteira')>()
  return {
    ...actual,
    buscarCarteira: vi.fn(),
    atualizarCarteira: vi.fn(),
  }
})

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'carteira-001' }),
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/carteiras/carteira-001',
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EditarCarteiraPage from './page'
import type { CarteiraResponse } from '@/features/carteira'

const carteiraFixture: CarteiraResponse = {
  id: 'carteira-001',
  contaId: 'conta-1',
  nome: 'Tesouro',
  tipo: 'RENDA_FIXA',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

function mockQueryCarteira(carteira: CarteiraResponse) {
  vi.mocked(useQuery).mockReturnValue({
    data: carteira,
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

describe('EditarCarteiraPage', () => {
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
    render(<EditarCarteiraPage />)
    expect(screen.getByText(/carregando/i)).toBeTruthy()
  })

  it('exibe mensagem de erro quando carteira nao encontrada', () => {
    mockQueryError()
    render(<EditarCarteiraPage />)
    expect(screen.getByText(/carteira nao encontrada/i)).toBeTruthy()
  })

  it('carrega dados da carteira via useQuery e popula o formulario', async () => {
    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    expect(screen.getByText('Editar Carteira')).toBeTruthy()
    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe('Tesouro')
    })
  })

  it('renderiza campos do formulario e botoes', () => {
    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText('Tipo')).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('chama mutate com valores atualizados ao submeter', async () => {
    const mockMutate = vi.fn()
    vi.mocked(useMutation).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useMutation>)

    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe('Tesouro')
    })

    const nomeInput = screen.getByLabelText(/nome/i)
    await userEvent.clear(nomeInput)
    await userEvent.type(nomeInput, 'Tesouro IPCA')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith(
        expect.objectContaining({ nome: 'Tesouro IPCA' }),
      )
    })
  })

  it('exibe texto Salvando... quando mutation esta pendente', () => {
    vi.mocked(useMutation).mockReturnValue({
      mutate: vi.fn(),
      isPending: true,
    } as unknown as ReturnType<typeof useMutation>)

    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    expect(screen.getByText(/salvando\.\.\./i)).toBeTruthy()
  })

  it('navega para /carteiras ao clicar em Cancelar', async () => {
    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/carteiras')
  })

  it('chama router.back() ao clicar no botao Voltar', async () => {
    mockQueryCarteira(carteiraFixture)
    render(<EditarCarteiraPage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })
})
