'use client'
import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { metaService } from '@/features/metas/services/meta-service'
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
import {
  StatusBadge,
  META_STATUS_CONFIG,
} from '@/shared/components/StatusBadge'
import { exportToCsv } from '@/shared/lib/export-csv'
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { Meta, StatusMeta } from '@/features/metas/types/meta'

const SCREEN_CODE = 'FIN-MET-001'

const FILTER_FIELDS: FilterFieldDef[] = [
  {
    name: 'status',
    label: 'Status',
    type: 'enum',
    options: [
      { value: 'EM_ANDAMENTO', label: 'Em andamento' },
      { value: 'CONCLUIDA', label: 'Concluida' },
      { value: 'CANCELADA', label: 'Cancelada' },
    ],
  },
]

const ENUM_FIELDS = new Set(['status'])

export default function MetasPage() {
  const router = useRouter()
  const [selecionada, setSelecionada] = useState<Meta | null>(null)
  const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([])

  const { data: rawData = [], isLoading, isError } = useQuery({
    queryKey: ['metas'],
    queryFn: metaService.listar,
  })

  const filtered = useMemo(() => {
    return rawData.filter((item) => {
      for (const f of activeFilters) {
        const fieldValue = String(
          (item as unknown as Record<string, unknown>)[f.field] ?? '',
        )
        if (ENUM_FIELDS.has(f.field)) {
          if (fieldValue !== f.value) return false
        } else if (!fieldValue.toLowerCase().includes(f.value.toLowerCase())) {
          return false
        }
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

  const columns: ColumnDef<Meta>[] = [
    {
      key: 'nome',
      label: 'Descricao',
      sortable: true,
      className: 'font-medium',
    },
    {
      key: 'valorAlvo',
      label: 'Valor Alvo',
      className: 'text-right tabular-nums',
      render: (_value, row) => formatBRL(row.valorAlvo.valor),
    },
    {
      key: 'valorAtual',
      label: 'Acumulado',
      className: 'text-right tabular-nums',
      render: (_value, row) => formatBRL(row.valorAtual.valor),
    },
    {
      key: 'status',
      label: 'Status',
      render: (value, row) => (
        <span className="flex items-center gap-2">
          <StatusBadge
            status={String(value)}
            config={META_STATUS_CONFIG}
            fallbackLabel={String(value)}
          />
          {row.atrasada && row.status === 'EM_ANDAMENTO' && (
            <Badge variant="destructive">Atrasada</Badge>
          )}
        </span>
      ),
    },
    {
      key: 'prazo',
      label: 'Prazo',
      sortable: true,
      render: (value) => formatDate(String(value)),
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'metas',
      filtered.map((m) => ({
        nome: m.nome,
        valorAlvo: m.valorAlvo.valor,
        valorAtual: m.valorAtual.valor,
        status: META_STATUS_CONFIG[m.status as StatusMeta]?.label ?? m.status,
        prazo: m.prazo,
      })),
      [
        { key: 'nome', label: 'Descricao' },
        { key: 'valorAlvo', label: 'Valor Alvo' },
        { key: 'valorAtual', label: 'Acumulado' },
        { key: 'status', label: 'Status' },
        { key: 'prazo', label: 'Prazo' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Metas</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="meta"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.nome}
            screenCode={SCREEN_CODE}
            onExportCsv={filtered.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/metas/novo')}>
            + Nova Meta
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
        <p className="text-sm text-destructive">Erro ao carregar metas.</p>
      )}

      {!isError && !isLoading && rawData.length === 0 && (
        <p className="text-muted-foreground">Nenhuma meta cadastrada.</p>
      )}

      {!isError && (isLoading || rawData.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={filtered}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma meta encontrada."
              onRowClick={(row) => router.push(`/metas/${row.id}`)}
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
