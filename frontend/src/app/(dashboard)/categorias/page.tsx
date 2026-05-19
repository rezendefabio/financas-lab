'use client'
import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import {
  FilterBar,
  type ActiveFilter,
  type FilterFieldDef,
} from '@/shared/components/FilterBar'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { exportToCsv } from '@/shared/lib/export-csv'
import { formatTipoCategoria } from '@/shared/lib/formatters'
import type { Categoria } from '@/features/categorias/types/categoria'

const SCREEN_CODE = 'FIN-CAT-001'

const FILTER_FIELDS: FilterFieldDef[] = [
  {
    name: 'tipo',
    label: 'Tipo',
    type: 'enum',
    options: [
      { value: 'RECEITA', label: 'Receita' },
      { value: 'DESPESA', label: 'Despesa' },
    ],
  },
]

export default function CategoriasPage() {
  const router = useRouter()
  const [selecionada, setSelecionada] = useState<Categoria | null>(null)
  const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([])

  // O filtro `tipo` e resolvido no servidor; demais filtros sao client-side.
  const tipoFiltro = activeFilters.find((f) => f.field === 'tipo')?.value as
    | 'RECEITA'
    | 'DESPESA'
    | undefined

  const { data: rawData = [], isLoading, isError } = useQuery({
    queryKey: ['categorias', tipoFiltro ?? null],
    queryFn: () =>
      tipoFiltro
        ? categoriasService.listarPorTipo(tipoFiltro)
        : categoriasService.listar(),
  })

  const nomePorId = useMemo(
    () => new Map(rawData.map((c) => [c.id, c.nome])),
    [rawData],
  )

  // O filtro `tipo` ja vai ao servidor; os demais (caso surjam) sao client-side.
  const filtered = useMemo(() => {
    const clientFilters = activeFilters.filter((f) => f.field !== 'tipo')
    return rawData.filter((item) => {
      for (const f of clientFilters) {
        const fieldValue = String(
          (item as unknown as Record<string, unknown>)[f.field] ?? '',
        ).toLowerCase()
        if (!fieldValue.includes(f.value.toLowerCase())) return false
      }
      return true
    })
  }, [rawData, activeFilters])

  const addFilter = (filter: ActiveFilter) => {
    setActiveFilters((prev) => [
      ...prev.filter((f) => f.field !== filter.field),
      filter,
    ])
  }
  const removeFilter = (field: string) => {
    setActiveFilters((prev) => prev.filter((f) => f.field !== field))
  }
  const clearFilters = () => setActiveFilters([])

  const columns = useMemo<ColumnDef<Categoria>[]>(
    () => [
      {
        key: 'nome',
        label: 'Nome',
        sortable: true,
        className: 'font-medium',
        render: (value, row) => (
          <span className="flex items-center gap-2">
            {String(value)}
            {row.system && <Badge variant="secondary">Sistema</Badge>}
          </span>
        ),
      },
      {
        key: 'tipo',
        label: 'Tipo',
        render: (value) => (
          <Badge variant={value === 'RECEITA' ? 'default' : 'destructive'}>
            {formatTipoCategoria(String(value))}
          </Badge>
        ),
      },
      {
        key: 'categoriaPaiId',
        label: 'Categoria Pai',
        className: 'text-muted-foreground text-sm',
        render: (value) => (value ? (nomePorId.get(String(value)) ?? '—') : '—'),
      },
    ],
    [nomePorId],
  )

  const handleExport = () => {
    exportToCsv(
      'categorias',
      filtered.map((c) => ({
        nome: c.nome,
        tipo: formatTipoCategoria(c.tipo),
        categoriaPai: c.categoriaPaiId
          ? (nomePorId.get(c.categoriaPaiId) ?? '')
          : '',
      })),
      [
        { key: 'nome', label: 'Nome' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'categoriaPai', label: 'Categoria Pai' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Categorias</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="categoria"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.nome}
            screenCode={SCREEN_CODE}
            onExportCsv={filtered.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/categorias/novo')}>
            Nova Categoria
          </Button>
        </div>
      </div>

      <FilterBar
        fields={FILTER_FIELDS}
        activeFilters={activeFilters}
        onAdd={addFilter}
        onRemove={removeFilter}
        onClear={clearFilters}
      />

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar categorias.</p>
      )}

      {!isError && !isLoading && rawData.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma categoria cadastrada.</p>
          <Button onClick={() => router.push('/categorias/novo')}>
            Criar primeira categoria
          </Button>
        </div>
      )}

      {!isError && (isLoading || rawData.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={filtered}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma categoria encontrada."
              onRowClick={(row) => router.push(`/categorias/${row.id}`)}
              rowActions={(row) => (
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`Historico de ${row.nome}`}
                  onClick={() => setSelecionada(row)}
                >
                  Log
                </Button>
              )}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
