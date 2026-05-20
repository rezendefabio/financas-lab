'use client'
import { useState, useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { lancamentoRecorrenteService } from '@/features/lancamentorecorrente'
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
import { formatBRL, formatDate } from '@/shared/lib/formatters'
import type { LancamentoRecorrente } from '@/features/lancamentorecorrente'

const SCREEN_CODE = 'FIN-REC-001'

const PERIODICIDADE_LABELS: Record<string, string> = {
  SEMANAL: 'Semanal',
  QUINZENAL: 'Quinzenal',
  MENSAL: 'Mensal',
  BIMESTRAL: 'Bimestral',
  TRIMESTRAL: 'Trimestral',
  SEMESTRAL: 'Semestral',
  ANUAL: 'Anual',
}

const TIPO_LABELS: Record<string, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
}

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
  { name: 'ativo', label: 'Ativo', type: 'boolean' },
]

const ENUM_FIELDS = new Set(['tipo'])
const BOOLEAN_FIELDS = new Set(['ativo'])

export default function LancamentosRecorrentesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionado, setSelecionado] = useState<LancamentoRecorrente | null>(
    null,
  )
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [activeFilters, setActiveFilters] = useState<ActiveFilter[]>([])

  const desativarMutation = useMutation({
    mutationFn: (id: string) => lancamentoRecorrenteService.desativar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['lancamentos-recorrentes'],
      })
      setConfirmDeleteId(null)
    },
  })

  const { data: rawData = [], isLoading, isError } = useQuery({
    queryKey: ['lancamentos-recorrentes'],
    queryFn: lancamentoRecorrenteService.listar,
  })

  const filtered = useMemo(() => {
    return rawData.filter((item) => {
      for (const f of activeFilters) {
        const record = item as unknown as Record<string, unknown>
        if (BOOLEAN_FIELDS.has(f.field)) {
          if (String(record[f.field]) !== f.operator) return false
        } else if (ENUM_FIELDS.has(f.field)) {
          if (String(record[f.field] ?? '') !== f.value) return false
        } else {
          const fieldValue = String(record[f.field] ?? '').toLowerCase()
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

  const columns: ColumnDef<LancamentoRecorrente>[] = [
    {
      key: 'descricao',
      label: 'Descricao',
      sortable: true,
      className: 'font-medium',
    },
    {
      key: 'valor',
      label: 'Valor',
      className: 'text-right tabular-nums',
      render: (_value, row) => formatBRL(row.valor.valor),
    },
    {
      key: 'tipo',
      label: 'Tipo',
      render: (value) => (
        <Badge variant={value === 'RECEITA' ? 'default' : 'secondary'}>
          {TIPO_LABELS[String(value)] ?? String(value)}
        </Badge>
      ),
    },
    {
      key: 'periodicidade',
      label: 'Periodicidade',
      render: (value) =>
        PERIODICIDADE_LABELS[String(value)] ?? String(value),
    },
    {
      key: 'proximaOcorrencia',
      label: 'Proxima Ocorrencia',
      render: (value) => formatDate(String(value)),
    },
    {
      key: 'ativo',
      label: 'Ativo',
      render: (value) => (
        <Badge variant={value ? 'default' : 'outline'}>
          {value ? 'Sim' : 'Nao'}
        </Badge>
      ),
    },
  ]

  const handleExport = () => {
    exportToCsv(
      'lancamentos-recorrentes',
      filtered.map((l) => ({
        descricao: l.descricao,
        valor: l.valor.valor,
        tipo: TIPO_LABELS[l.tipo] ?? l.tipo,
        periodicidade: PERIODICIDADE_LABELS[l.periodicidade] ?? l.periodicidade,
        proximaOcorrencia: l.proximaOcorrencia,
        ativo: l.ativo ? 'Sim' : 'Nao',
      })),
      [
        { key: 'descricao', label: 'Descricao' },
        { key: 'valor', label: 'Valor' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'periodicidade', label: 'Periodicidade' },
        { key: 'proximaOcorrencia', label: 'Proxima Ocorrencia' },
        { key: 'ativo', label: 'Ativo' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">
          Lancamentos Recorrentes
        </h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="lancamento-recorrente"
            entityId={selecionado?.id ?? null}
            entityLabel={selecionado?.descricao}
            screenCode={SCREEN_CODE}
            onExportCsv={filtered.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/lancamentos-recorrentes/novo')}>
            + Novo Lancamento Recorrente
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
        <p className="text-sm text-destructive">
          Erro ao carregar lancamentos recorrentes.
        </p>
      )}

      {!isError && !isLoading && rawData.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">
            Nenhum lancamento recorrente cadastrado.
          </p>
          <Button onClick={() => router.push('/lancamentos-recorrentes/novo')}>
            Criar primeiro lancamento recorrente
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
              emptyMessage="Nenhum lancamento recorrente encontrado."
              onRowClick={(row) =>
                router.push(`/lancamentos-recorrentes/${row.id}`)
              }
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
                      aria-label={`Historico de ${row.descricao}`}
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
