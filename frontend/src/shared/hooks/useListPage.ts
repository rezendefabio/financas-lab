'use client'

/**
 * useListPage -- orquestra estado de listagem (ADR-014, fase UI-5).
 *
 * Concentra filtros, paginacao e ordenacao de uma tela CRUD, persistindo tudo
 * na URL via `useSearchParams`/`useRouter` e disparando o fetch via TanStack
 * Query. Formato da URL:
 *
 *   ?filtros=campo:operador:valor,campo2:operador2:valor2&page=0&sort=campo:dir
 *
 * Cada filtro carrega um operador (ver `OPERATORS_BY_TYPE` em `FilterBar`). O
 * valor pode estar URI-encoded para suportar `:` e `,`. Filtros booleanos tem
 * valor vazio (`campo:true:`). O `displayValue`/`operatorLabel` de cada chip nao
 * vao para a URL; sao reconstruidos pelo consumidor a partir das definicoes de
 * campo da `<FilterBar>`.
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

/** Filtro persistido: operador + valor cru. */
export interface FilterEntry {
  operator: string
  value: string
}

/** Mapa de filtros indexado por nome de campo. */
export type FilterMap = Record<string, FilterEntry>

export interface UseListPageOptions<TFilters extends Record<string, string>> {
  queryKey: string
  fetcher: (params: {
    /** Mapa campo -> valor cru (compat retroativa; ignora operadores). */
    filters: TFilters
    /** Lista completa de filtros com operador. */
    activeFilters: ActiveFilter[]
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

/**
 * Le `?filtros=campo:operador:valor,...` da URL como mapa campo -> entrada.
 *
 * Aceita tambem o formato legado `campo:valor` (sem operador): nesse caso o
 * operador assume `eq`. O valor e URI-decodificado.
 */
function parseFiltros(raw: string | null): FilterMap {
  if (!raw) return {}
  const result: FilterMap = {}
  for (const pair of raw.split(',')) {
    if (!pair) continue
    const parts = pair.split(':')
    const field = (parts[0] ?? '').trim()
    if (!field) continue
    if (parts.length >= 3) {
      const operator = (parts[1] ?? '').trim()
      const value = decodeValue(parts.slice(2).join(':'))
      result[field] = { operator, value }
    } else if (parts.length === 2) {
      // Formato legado campo:valor -- operador implicito `eq`.
      result[field] = { operator: 'eq', value: decodeValue(parts[1]) }
    }
  }
  return result
}

/** Serializa um mapa de filtros no formato `campo:operador:valor,...`. */
function serializeFiltros(filtros: FilterMap): string {
  return Object.entries(filtros)
    .map(([field, entry]) => `${field}:${entry.operator}:${encodeValue(entry.value)}`)
    .join(',')
}

/** Codifica o valor para a URL, escapando `:` e `,` que sao separadores. */
function encodeValue(value: string): string {
  return value.replace(/%/g, '%25').replace(/:/g, '%3A').replace(/,/g, '%2C')
}

/** Decodifica um valor de filtro vindo da URL. */
function decodeValue(value: string): string {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
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
  const filtros = useMemo<FilterMap>(() => {
    const fromUrl = parseFiltros(searchParams.get('filtros'))
    if (Object.keys(fromUrl).length > 0) return fromUrl
    // defaultFilters chega como mapa campo -> valor; operador implicito `eq`.
    const defaults = (defaultFilters ?? {}) as Record<string, string>
    const mapped: FilterMap = {}
    for (const [field, value] of Object.entries(defaults)) {
      mapped[field] = { operator: 'eq', value }
    }
    return mapped
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
    (next: { filtros?: FilterMap; page?: number; sort?: string | null }) => {
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
        filtros: {
          ...filtros,
          [filter.field]: { operator: filter.operator, value: filter.value },
        },
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

  const sortParam = sort ? `${sort.field}:${sort.dir}` : undefined

  // Lista de filtros ativos (com operador) para o consumidor e o fetcher.
  const activeFilters: ActiveFilter[] = useMemo(
    () =>
      Object.entries(filtros).map(([field, entry]) => ({
        field,
        operator: entry.operator,
        label: field,
        value: entry.value,
        displayValue: entry.value,
        operatorLabel: entry.operator,
      })),
    [filtros],
  )

  // Mapa campo -> valor cru para compatibilidade retroativa do fetcher.
  const filtersValueMap = useMemo(() => {
    const map: Record<string, string> = {}
    for (const [field, entry] of Object.entries(filtros)) {
      map[field] = entry.value
    }
    return map
  }, [filtros])

  const query = useQuery({
    queryKey: [queryKey, filtros, page, pageSize, sortParam],
    queryFn: () =>
      fetcher({
        filters: filtersValueMap as TFilters,
        activeFilters,
        page,
        size: pageSize,
        sort: sortParam,
      }),
  })

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
