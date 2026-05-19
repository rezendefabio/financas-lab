import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useListPage, type PageResponse } from './useListPage'

const replaceMock = vi.fn()
let currentParams = new URLSearchParams()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: replaceMock }),
  usePathname: () => '/transacoes',
  useSearchParams: () => currentParams,
}))

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const page: PageResponse<{ id: string }> = {
  content: [{ id: 'a' }, { id: 'b' }],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 20,
}

describe('useListPage', () => {
  beforeEach(() => {
    replaceMock.mockClear()
    currentParams = new URLSearchParams()
  })

  it('inicia sem filtros e na pagina 0', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    expect(result.current.page).toBe(0)
    expect(result.current.activeFilters).toEqual([])
    await waitFor(() => expect(result.current.data).toHaveLength(2))
  })

  it('le formato legado campo:valor da URL com operador eq implicito', async () => {
    currentParams = new URLSearchParams('filtros=tipo:RECEITA&page=2')
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    expect(result.current.page).toBe(2)
    expect(result.current.activeFilters).toEqual([
      {
        field: 'tipo',
        operator: 'eq',
        operatorLabel: 'eq',
        label: 'tipo',
        value: 'RECEITA',
        displayValue: 'RECEITA',
      },
    ])
  })

  it('le formato campo:operador:valor da URL', async () => {
    currentParams = new URLSearchParams('filtros=descricao:contains:mercado')
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    expect(result.current.activeFilters).toEqual([
      {
        field: 'descricao',
        operator: 'contains',
        operatorLabel: 'contains',
        label: 'descricao',
        value: 'mercado',
        displayValue: 'mercado',
      },
    ])
  })

  it('addFilter serializa campo:operador:valor na URL e volta para a pagina 0', async () => {
    currentParams = new URLSearchParams('page=3')
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.addFilter({
        field: 'descricao',
        operator: 'contains',
        operatorLabel: 'contem',
        label: 'Descricao',
        value: 'mercado',
        displayValue: 'mercado',
      })
    })

    expect(replaceMock).toHaveBeenCalledTimes(1)
    const url = replaceMock.mock.calls[0][0] as string
    expect(decodeURIComponent(url)).toContain('filtros=descricao:contains:mercado')
    expect(url).not.toContain('page=')
  })

  it('serializa filtro boolean com valor vazio (campo:operador:)', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.addFilter({
        field: 'ativa',
        operator: 'true',
        operatorLabel: 'verdadeiro',
        label: 'Ativa',
        value: '',
        displayValue: 'verdadeiro',
      })
    })

    const url = replaceMock.mock.calls[0][0] as string
    expect(decodeURIComponent(url)).toContain('filtros=ativa:true:')
  })

  it('URI-encoda valor que contem o separador `:`', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.addFilter({
        field: 'descricao',
        operator: 'contains',
        operatorLabel: 'contem',
        label: 'Descricao',
        value: 'a:b',
        displayValue: 'a:b',
      })
    })

    const url = replaceMock.mock.calls[0][0] as string
    // O `:` do valor fica encoded para nao colidir com o separador.
    expect(url).toContain('descricao%3Acontains%3Aa%253Ab')
  })

  it('removeFilter remove o campo da URL', async () => {
    currentParams = new URLSearchParams('filtros=tipo:RECEITA,status:CLEARED')
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.removeFilter('tipo')
    })

    const url = replaceMock.mock.calls[0][0] as string
    expect(decodeURIComponent(url)).toContain('status:eq:CLEARED')
    expect(url).not.toContain('tipo')
  })

  it('clearFilters limpa o parametro filtros da URL', async () => {
    currentParams = new URLSearchParams('filtros=tipo:RECEITA')
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.clearFilters()
    })

    const url = replaceMock.mock.calls[0][0] as string
    expect(url).not.toContain('filtros')
  })

  it('setPage atualiza o parametro page na URL', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.setPage(4)
    })

    const url = replaceMock.mock.calls[0][0] as string
    expect(url).toContain('page=4')
  })

  it('setSort grava o parametro sort no formato campo:dir', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
        }),
      { wrapper: makeWrapper() },
    )

    act(() => {
      result.current.setSort('data', 'desc')
    })

    const url = replaceMock.mock.calls[0][0] as string
    expect(url).toContain('sort=data%3Adesc')
  })

  it('aplica defaultSort quando a URL nao traz sort', async () => {
    const fetcher = vi.fn().mockResolvedValue(page)
    const { result } = renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
          defaultSort: { field: 'data', dir: 'desc' },
        }),
      { wrapper: makeWrapper() },
    )

    expect(result.current.sort).toEqual({ field: 'data', dir: 'desc' })
  })

  it('passa filtros, page, size e sort para o fetcher', async () => {
    currentParams = new URLSearchParams('filtros=tipo:RECEITA&page=1')
    const fetcher = vi.fn().mockResolvedValue(page)
    renderHook(
      () =>
        useListPage<{ id: string }, Record<string, string>>({
          queryKey: 'transacoes',
          fetcher,
          pageSize: 10,
          defaultSort: { field: 'data', dir: 'desc' },
        }),
      { wrapper: makeWrapper() },
    )

    await waitFor(() => expect(fetcher).toHaveBeenCalled())
    expect(fetcher).toHaveBeenCalledWith({
      filters: { tipo: 'RECEITA' },
      activeFilters: [
        {
          field: 'tipo',
          operator: 'eq',
          operatorLabel: 'eq',
          label: 'tipo',
          value: 'RECEITA',
          displayValue: 'RECEITA',
        },
      ],
      page: 1,
      size: 10,
      sort: 'data:desc',
    })
  })
})
