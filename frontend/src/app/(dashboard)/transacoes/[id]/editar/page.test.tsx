import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Transacao } from '@/features/transacoes/types/transacao'

const buscarPorId = vi.fn()
const editar = vi.fn()
vi.mock('@/features/transacoes/services/transacoes.service', () => ({
  transacoesService: {
    buscarPorId: (...args: unknown[]) => buscarPorId(...args),
    editar: (...args: unknown[]) => editar(...args),
  },
}))

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: { listar: vi.fn() },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: { listar: vi.fn() },
}))

vi.mock('@/features/payee/services/payee-service', () => ({
  listarPayees: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/features/tag/services/tag-service', () => ({
  listarTags: vi.fn().mockResolvedValue([]),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'tx-1' }),
  usePathname: () => '/transacoes/tx-1/editar',
}))

const transacao: Transacao = {
  id: 'tx-1',
  tipo: 'DESPESA',
  valor: 150,
  moeda: 'BRL',
  data: '2026-05-10',
  descricao: 'Supermercado',
  contaId: '359a0532-0548-4757-9f54-68a78abbad33',
  contaDestinoId: null,
  categoriaId: null,
  criadoEm: '2026-05-10T00:00:00Z',
  atualizadoEm: '2026-05-10T00:00:00Z',
  status: 'CLEARED',
  payeeId: null,
  tagIds: [],
  transferGroupId: null,
}

// Estado controlavel das queries: a primeira chamada (transacao) varia por teste.
let transacaoQueryState: { data: unknown; isLoading: boolean; isError: boolean } = {
  data: transacao,
  isLoading: false,
  isError: false,
}

const mockInvalidateQueries = vi.fn()
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
    useQuery: vi.fn((opts: { queryKey: unknown[] }) => {
      // A pagina de edicao usa ['transacao', id]; o form usa contas/categorias/payees/tags.
      if (Array.isArray(opts.queryKey) && opts.queryKey[0] === 'transacao') {
        return transacaoQueryState
      }
      return { data: undefined, isLoading: false }
    }),
    useMutation: vi.fn(({ mutationFn, onSuccess, onError }: {
      mutationFn: (data: unknown) => Promise<unknown>
      onSuccess: (result: unknown) => Promise<void>
      onError: (err: unknown) => void
    }) => ({
      mutate: vi.fn(async (data: unknown) => {
        try {
          const result = await mutationFn(data)
          await onSuccess(result)
        } catch (err) {
          onError(err)
        }
      }),
      isPending: false,
    })),
    useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
  }
})

import EditarTransacaoPage from './page'

describe('EditarTransacaoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    transacaoQueryState = { data: transacao, isLoading: false, isError: false }
  })

  it('renderiza o titulo da pagina', () => {
    render(<EditarTransacaoPage />)
    expect(screen.getByText('Editar Transacao')).toBeTruthy()
  })

  it('pre-preenche o formulario com a descricao existente', () => {
    render(<EditarTransacaoPage />)
    expect(screen.getByDisplayValue('Supermercado')).toBeTruthy()
  })

  it('renderiza botao Salvar quando os dados carregam', () => {
    render(<EditarTransacaoPage />)
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
  })

  it('renderiza skeleton enquanto carrega', () => {
    transacaoQueryState = { data: undefined, isLoading: true, isError: false }
    render(<EditarTransacaoPage />)
    expect(screen.queryByRole('button', { name: /salvar/i })).toBeNull()
  })

  it('exibe mensagem de erro quando a transacao nao e encontrada', () => {
    transacaoQueryState = { data: undefined, isLoading: false, isError: true }
    render(<EditarTransacaoPage />)
    expect(screen.getByText(/nao encontrada/i)).toBeTruthy()
  })

  it('navega para /transacoes ao clicar em Cancelar', async () => {
    render(<EditarTransacaoPage />)
    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(mockPush).toHaveBeenCalledWith('/transacoes')
  })

  it('chama transacoesService.editar e navega ao submeter', async () => {
    editar.mockResolvedValue(transacao)
    render(<EditarTransacaoPage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(editar).toHaveBeenCalledWith('tx-1', expect.objectContaining({
        descricao: 'Supermercado',
        contaId: '359a0532-0548-4757-9f54-68a78abbad33',
      }))
    })
    expect(mockPush).toHaveBeenCalledWith('/transacoes')
  })

  it('renderiza botao Voltar com aria-label', () => {
    render(<EditarTransacaoPage />)
    expect(screen.getByRole('button', { name: /voltar/i })).toBeTruthy()
  })
})
