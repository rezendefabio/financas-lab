'use client'

/**
 * DataTable -- tabela generica de listagem (ADR-014, fase UI-5).
 *
 * Componente puro de apresentacao: nao conhece nenhum tipo de dominio. Recebe
 * `data` + `columns` e renderiza um `<Table>` shadcn com ordenacao por coluna,
 * clique de linha, coluna de acoes, estado de loading (skeleton) e estado vazio.
 */

import * as React from 'react'
import { ChevronDown, ChevronsUpDown, ChevronUp } from 'lucide-react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'

/** Definicao de uma coluna da tabela. */
export interface ColumnDef<T> {
  /** Campo de `T` (ou string livre) que identifica a coluna. */
  key: keyof T | string
  /** Texto exibido no header. */
  label: string
  /** Se `true`, o header vira clicavel e dispara `onSortChange`. */
  sortable?: boolean
  /** Classe CSS aplicada a celula (e ao header). */
  className?: string
  /** Renderizador customizado da celula. Default: `String(value)`. */
  render?: (value: unknown, row: T) => React.ReactNode
}

export interface DataTableProps<T> {
  data: T[]
  columns: ColumnDef<T>[]
  /** Campo de identificador unico, usado como `key` de cada linha. */
  keyField: keyof T
  isLoading?: boolean
  emptyMessage?: string
  sort?: { field: string; dir: 'asc' | 'desc' }
  onSortChange?: (field: string, dir: 'asc' | 'desc') => void
  onRowClick?: (row: T) => void
  /** Renderiza acoes por linha numa ultima coluna "Acoes" alinhada a direita. */
  rowActions?: (row: T) => React.ReactNode
}

/** Icone de ordenacao conforme o estado atual de `sort` para a coluna. */
function SortIcon({
  active,
  dir,
}: {
  active: boolean
  dir: 'asc' | 'desc'
}) {
  if (!active) {
    return <ChevronsUpDown className="ml-1 inline h-3.5 w-3.5 text-muted-foreground" />
  }
  return dir === 'asc' ? (
    <ChevronUp className="ml-1 inline h-3.5 w-3.5" />
  ) : (
    <ChevronDown className="ml-1 inline h-3.5 w-3.5" />
  )
}

export function DataTable<T>({
  data,
  columns,
  keyField,
  isLoading = false,
  emptyMessage = 'Nenhum registro encontrado.',
  sort,
  onSortChange,
  onRowClick,
  rowActions,
}: DataTableProps<T>) {
  const totalCols = columns.length + (rowActions ? 1 : 0)

  const handleSort = (field: string) => {
    if (!onSortChange) return
    const nextDir: 'asc' | 'desc' =
      sort?.field === field && sort.dir === 'asc' ? 'desc' : 'asc'
    onSortChange(field, nextDir)
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          {columns.map((col) => {
            const field = String(col.key)
            const isActive = sort?.field === field
            return (
              <TableHead
                key={field}
                className={cn(col.className, col.sortable && 'cursor-pointer select-none')}
                onClick={col.sortable ? () => handleSort(field) : undefined}
                aria-sort={
                  col.sortable
                    ? isActive
                      ? sort?.dir === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : 'none'
                    : undefined
                }
              >
                {col.label}
                {col.sortable && (
                  <SortIcon active={isActive} dir={isActive ? sort!.dir : 'asc'} />
                )}
              </TableHead>
            )
          })}
          {rowActions && <TableHead className="text-right">Acoes</TableHead>}
        </TableRow>
      </TableHeader>
      <TableBody>
        {isLoading &&
          Array.from({ length: Math.min(data.length || 3, 5) }).map((_, i) => (
            <TableRow key={`skeleton-${i}`}>
              {columns.map((col) => (
                <TableCell key={String(col.key)} className={col.className}>
                  <Skeleton className="h-4 w-24" />
                </TableCell>
              ))}
              {rowActions && (
                <TableCell className="text-right">
                  <Skeleton className="ml-auto h-4 w-16" />
                </TableCell>
              )}
            </TableRow>
          ))}

        {!isLoading && data.length === 0 && (
          <TableRow>
            <TableCell
              colSpan={totalCols}
              className="py-10 text-center text-muted-foreground"
            >
              {emptyMessage}
            </TableCell>
          </TableRow>
        )}

        {!isLoading &&
          data.map((row) => (
            <TableRow
              key={String(row[keyField])}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={cn(onRowClick && 'cursor-pointer hover:bg-muted/50')}
            >
              {columns.map((col) => {
                const value = (row as Record<string, unknown>)[String(col.key)]
                return (
                  <TableCell key={String(col.key)} className={col.className}>
                    {col.render ? col.render(value, row) : String(value ?? '')}
                  </TableCell>
                )
              })}
              {rowActions && (
                <TableCell
                  className="text-right"
                  onClick={(e) => e.stopPropagation()}
                >
                  {rowActions(row)}
                </TableCell>
              )}
            </TableRow>
          ))}
      </TableBody>
    </Table>
  )
}
