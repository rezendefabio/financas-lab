'use client'
import { useInfiniteQuery } from '@tanstack/react-query'
import { auditlogService } from '../services/auditlog-service'
import type { AuditLogEntry } from '../types/auditlog'

const PAGE_SIZE = 20

/**
 * Carrega a trilha de auditoria de uma entidade, paginada.
 *
 * Fica desabilitado enquanto `entityId` for null/undefined (drawer fechado).
 * As paginas sao acumuladas: `entries` contem todas as ja carregadas e
 * `fetchNextPage` anexa a proxima.
 */
export function useAuditLog(entityType: string, entityId: string | null | undefined) {
  const query = useInfiniteQuery({
    queryKey: ['audit-log', entityType, entityId],
    enabled: !!entityId,
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      auditlogService.listarPorEntidade(entityType, entityId as string, pageParam, PAGE_SIZE),
    getNextPageParam: (lastPage) =>
      lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
  })

  const entries: AuditLogEntry[] =
    query.data?.pages.flatMap((page) => page.content) ?? []

  return {
    entries,
    isLoading: query.isLoading,
    isError: query.isError,
    hasNextPage: query.hasNextPage,
    isFetchingNextPage: query.isFetchingNextPage,
    fetchNextPage: query.fetchNextPage,
  }
}
