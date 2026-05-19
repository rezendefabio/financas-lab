'use client'

/**
 * useListPage -- orquestra estado de listagem (ADR-014, fase UI-5).
 *
 * Concentra filtros, paginacao e ordenacao de uma tela CRUD, persistindo tudo
 * na URL via `useSearchParams`/`useRouter` e disparando o fetch via TanStack
 * Query. Formato da URL:
 *
 *   ?filtros=campo1:valor1,campo2:valor2&page=0&sort=campo:dir
 *
 * O `displayValue` de cada chip nao vai para a URL; e reconstruido pelo
 * consumidor a partir das definicoes de campo da `<FilterBar>`.
 */

import { useCallback, useMemo } from 'react'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import type { ActiveFilter } from '@/shared/components/FilterBar'

/** Resposta paginada padrao do backend (Spring `Page`). */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface UseListPageOptions<TFilters extends Record<string, string>> {
  queryKey: string
  fetcher: (params: {
    filters: TFilters
    page: number
    size: number
    sort?: string
  }) => Promise<PageResponse<unknown>>
  defaultFilters?: Partial<TFilters>
  /** Tamanho da pagina. Default: 20. */
  pageSize?: number
  defaultSort?: { field: string; dir: 'asc' | 'desc' }
}

export interface UseListPageResult<TData> {
  data: TData[]
  totalElements: number
  totalPages: number
  page: number
  sort: { field: string; dir: 'asc' | 'desc' } | undefined
  isLoading: boolean
  isError: boolean
  activeFilters: ActiveFilter[]
  addFilter: (filter: ActiveFilter) => void
  removeFilter: (fieldName: string) => void
  clearFilters: () => void
  setPage: (page: number) => void
  setSort: (field: string, dir: 'asc' | 'desc') => void
}

/** Le `?filtros=a:1,b:2` da URL como mapa campo -> valor. */
function parseFiltros(raw: string | null): Record<string, string> {
  if (!raw) return {}
  const result: Record<string, string> = {}
  for (const pair of raw.split(',')) {
    const idx = pair.indexOf(':')
    if (idx <= 0) continue
    const field = pair.slice(0, idx).trim()
    const value = pair.slice(idx + 1).trim()
    if (field) result[field] = value
  }
  return result
}

/** Serializa um mapa campo -> valor no formato `a:1,b:2`. */
function serializeFiltros(filtros: Record<string, string>): string {
  return Object.entries(filtros)
    .map(([field, value]) => `${field}:${value}`)
    .join(',')
}

/** Le `?sort=campo:dir` da URL. */
function parseSort(
  raw: string | null,
): { field: string; dir: 'asc' | 'desc' } | undefined {
  if (!raw) return undefined
  const idx = raw.indexOf(':')
  if (idx <= 0) return undefined
  const field = raw.slice(0, idx).trim()
  const dir = raw.slice(idx + 1).trim()
  if (!field) return undefined
  return { field, dir: dir === 'desc' ? 'desc' : 'asc' }
}

export function useListPage<
  TData,
  TFilters extends Record<string, string>,
>(options: UseListPageOptions<TFilters>): UseListPageResult<TData> {
  const { queryKey, fetcher, defaultFilters, pageSize = 20, defaultSort } =
    options

  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  // Estado derivado da URL (fonte unica da verdade).
  const filtros = useMemo(() => {
    const fromUrl = parseFiltros(searchParams.get('filtros'))
    return Object.keys(fromUrl).length > 0
      ? fromUrl
      : { ...(defaultFilters as Record<string, string> | undefined) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  const page = useMemo(() => {
    const raw = Number(searchParams.get('page'))
    return Number.isFinite(raw) && raw >= 0 ? raw : 0
  }, [searchParams])

  const sort = useMemo(() => {
    return parseSort(searchParams.get('sort')) ?? defaultSort
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  /** Reescreve a URL preservando os parametros nao gerenciados pelo hook. */
  const writeUrl = useCallback(
    (next: { filtros?: Record<string, string>; page?: number; sort?: string | null }) => {
      const params = new URLSearchParams(searchParams.toString())

      if (next.filtros !== undefined) {
        const serialized = serializeFiltros(next.filtros)
        if (serialized) params.set('filtros', serialized)
        else params.delete('filtros')
      }
      if (next.page !== undefined) {
        if (next.page > 0) params.set('page', String(next.page))
        else params.delete('page')
      }
      if (next.sort !== undefined) {
        if (next.sort) params.set('sort', next.sort)
        else params.delete('sort')
      }

      const query = params.toString()
      router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false })
    },
    [router, pathname, searchParams],
  )

  const addFilter = useCallback(
    (filter: ActiveFilter) => {
      writeUrl({
        filtros: { ...filtros, [filter.field]: filter.value },
        page: 0,
      })
    },
    [filtros, writeUrl],
  )

  const removeFilter = useCallback(
    (fieldName: string) => {
      const next = { ...filtros }
      delete next[fieldName]
      writeUrl({ filtros: next, page: 0 })
    },
    [filtros, writeUrl],
  )

  const clearFilters = useCallback(() => {
    writeUrl({ filtros: {}, page: 0 })
  }, [writeUrl])

  const setPage = useCallback(
    (nextPage: number) => {
      writeUrl({ page: nextPage })
    },
    [writeUrl],
  )

  const setSort = useCallback(
    (field: string, dir: 'asc' | 'desc') => {
      writeUrl({ sort: `${field}:${dir}`, page: 0 })
    },
    [writeUrl],
  )

  const sortParam = sort ? `${sort.field},${sort.dir}` : undefined

  const query = useQuery({
    queryKey: [queryKey, filtros, page, pageSize, sortParam],
    queryFn: () =>
      fetcher({
        filters: filtros as TFilters,
        page,
        size: pageSize,
        sort: sortParam,
      }),
  })

  const activeFilters: ActiveFilter[] = useMemo(
    () =>
      Object.entries(filtros).map(([field, value]) => ({
        field,
        label: field,
        value,
        displayValue: value,
      })),
    [filtros],
  )

  return {
    data: (query.data?.content ?? []) as TData[],
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    page,
    sort,
    isLoading: query.isLoading,
    isError: query.isError,
    activeFilters,
    addFilter,
    removeFilter,
    clearFilters,
    setPage,
    setSort,
  }
}
