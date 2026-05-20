'use client'
import { useState, useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
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
import { formatBRL } from '@/shared/lib/formatters'
import type { Orcamento } from '@/features/orcamentos/types/orcamento'

const SCREEN_CODE = 'FIN-ORC-001'

const FILTER_FIELDS: FilterFieldDef[] = [
  { name: 'ativo', label: 'Ativo', type: 'boolean' },
]

const BOOLEAN_FIELDS = new Set(['ativo'])

/** mesAno = "YYYY-MM-01" -> "MM/YYYY". */
function formatMesAno(mesAno: string): string {
  const [year, month] = mesAno.split('-')
  return `${month}/${year}`
}

export default function OrcamentosPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionado, setSelecionado] = useState<Orcamento | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([])

  const desativarMutation = useMutation({
    mutationFn: (id: string) => orcamentoService.desativar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orcamentos'] })
      setConfirmDeleteId(null)
    },
  })

  const { data: rawData = [], isLoading, isError } = useQuery({
    queryKey: ['orcamentos'],
    queryFn: orcamentoService.listar,
  })

  const { data: categorias = [] } = useQuery({
    queryKey: ['categorias'],
    queryFn: () => categoriasService.listar(),
  })

  const categoriasMap = useMemo(
    () => new Map(categorias.map((c) => [c.id, c.nome])),
    [categorias],
  )

  const filtered = useMemo(() => {
    return rawData.filter((item) => {
      for (const f of activeFilters) {
        if (BOOLEAN_FIELDS.has(f.field)) {
          const fieldValue = String(
            (item as unknown as Record<string, unknown>)[f.field],
          )
          if (fieldValue !== f.operator) return false
        } else {
          const fieldValue = String(
            (item as unknown as Record<string, unknown>)[f.field] ?? '',
          ).toLowerCase()
          if (!fieldValue.includes(f.value.toLowerCase())) return false
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

  const columns: ColumnDef<Orcamento>[] = [
    {
      key: 'categoriaId',
      label: 'Categoria',
      className: 'font-medium',
      render: (value) => categoriasMap.get(String(value)) ?? String(value),
    },
    {
      key: 'valorLimite',
      label: 'Limite',
      className: 'text-right tabular-nums',
      render: (_value, row) => formatBRL(row.valorLimite.valor),
    },
    {
      key: 'mesAno',
      label: 'Mes/Ano',
      render: (value) => formatMesAno(String(value)),
    },
    {
      key: 'ativo',
      label: 'Ativo',
      render: (value) => (
        <Badge variant={value ? 'default' : 'secondary'}>
          {value ? 'Sim' : 'Nao'}
        </Badge>
      ),
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'orcamentos',
      filtered.map((o) => ({
        categoria: categoriasMap.get(o.categoriaId) ?? o.categoriaId,
        limite: o.valorLimite.valor,
        mesAno: formatMesAno(o.mesAno),
        ativo: o.ativo ? 'Sim' : 'Nao',
      })),
      [
        { key: 'categoria', label: 'Categoria' },
        { key: 'limite', label: 'Limite' },
        { key: 'mesAno', label: 'Mes/Ano' },
        { key: 'ativo', label: 'Ativo' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Orcamentos</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="orcamento"
            entityId={selecionado?.id ?? null}
            entityLabel={
              selecionado
                ? (categoriasMap.get(selecionado.categoriaId) ??
                  selecionado.categoriaId)
                : undefined
            }
            screenCode={SCREEN_CODE}
            onExportCsv={filtered.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/orcamentos/novo')}>
            + Novo Orcamento
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
        <p className="text-sm text-destructive">Erro ao carregar orcamentos.</p>
      )}

      {!isError && !isLoading && rawData.length === 0 && (
        <p className="text-muted-foreground">Nenhum orcamento cadastrado.</p>
      )}

      {!isError && (isLoading || rawData.length > 0) && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={filtered}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhum orcamento encontrado."
              onRowClick={(row) => router.push(`/orcamentos/${row.id}`)}
              rowActions={(row) =>
                confirmDeleteId === row.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => desativarMutation.mutate(row.id)}
                      disabled={desativarMutation.isPending}
                    >
                      Confirmar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => setConfirmDeleteId(null)}
                    >
                      Cancelar
                    </Button>
                  </span>
                ) : (
                  <span className="inline-flex gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={`Historico de orcamento ${row.id}`}
                      onClick={() => setSelecionado(row)}
                    >
                      Log
                    </Button>
                    {row.ativo && (
                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => setConfirmDeleteId(row.id)}
                      >
                        Desativar
                      </Button>
                    )}
                  </span>
                )
              }
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
