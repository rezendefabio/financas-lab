'use client'
import { useState, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import { History, Pencil, Trash2 } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { DataTable, type ColumnDef } from '@/shared/components/DataTable'
import {
  FilterBar,
  OPERATORS_BY_TYPE,
  type ActiveFilter,
  type FilterFieldDef,
} from '@/shared/components/FilterBar'
import { ActionsPanel } from '@/shared/components/ActionsPanel'
import { useListPage } from '@/shared/hooks/useListPage'
import { StatusBadge, type StatusConfig } from '@/shared/components/StatusBadge'
import { exportToCsv } from '@/shared/lib/export-csv'
import { formatBRL, formatTipoTransacao, formatDate } from '@/shared/lib/formatters'
import type { StatusTransacao, Transacao } from '@/features/transacoes/types/transacao'

const SCREEN_CODE = 'FIN-TRX-001'

const STATUS_TRANSACAO_CONFIG: Record<StatusTransacao, StatusConfig> = {
  CLEARED: { label: 'Confirmada', variant: 'default' },
  PENDING: { label: 'Pendente', variant: 'secondary' },
  SCHEDULED: { label: 'Agendada', variant: 'outline' },
  CANCELLED: { label: 'Cancelada', variant: 'destructive' },
}

const FILTER_FIELDS_BASE: FilterFieldDef[] = [
  {
    name: 'tipo',
    label: 'Tipo',
    type: 'enum',
    options: [
      { value: 'RECEITA', label: 'Receita' },
      { value: 'DESPESA', label: 'Despesa' },
      { value: 'TRANSFERENCIA', label: 'Transferencia' },
    ],
  },
  {
    name: 'status',
    label: 'Status',
    type: 'enum',
    options: [
      { value: 'CLEARED', label: 'Confirmada' },
      { value: 'PENDING', label: 'Pendente' },
      { value: 'SCHEDULED', label: 'Agendada' },
      { value: 'CANCELLED', label: 'Cancelada' },
    ],
  },
  { name: 'descricao', label: 'Descricao', type: 'string' },
  { name: 'valor', label: 'Valor', type: 'number' },
  { name: 'data', label: 'Data', type: 'date' },
]

/** Reconstroi o `displayValue` de cada chip a partir das definicoes de campo. */
function resolveDisplay(field: string, value: string, fields: FilterFieldDef[]): string {
  const def = fields.find((f) => f.name === field)
  if (!def) return value
  if (def.type === 'enum') {
    return def.options?.find((o) => o.value === value)?.label ?? value
  }
  if (def.type === 'number') {
    const n = Number(value)
    return Number.isFinite(n) ? formatBRL(n) : value
  }
  return value
}

/** Rotulo humano de um operador, para reconstruir o chip a partir da URL. */
function resolveOperatorLabel(
  field: string,
  operator: string,
  fields: FilterFieldDef[],
): string {
  const def = fields.find((f) => f.name === field)
  if (!def) return operator
  return (
    OPERATORS_BY_TYPE[def.type].find((o) => o.value === operator)?.label ??
    operator
  )
}

/** Campos que viajam como filtros adicionais (`filtros=campo:op:valor`). */
const CAMPOS_FILTRO_ADICIONAL = new Set(['descricao', 'valor'])

/**
 * Traduz os filtros ativos da `FilterBar` nos parametros aceitos pelo backend.
 *
 * - `tipo`/`status`/`contaId`: parametros enum diretos (operador `eq` implicito).
 * - `data`: vira `dataInicio`/`dataFim` conforme o operador (`gte`/`lte`/`eq`).
 * - `descricao`/`valor`: serializados em `filtros=campo:operador:valor`.
 */
function buildBackendParams(activeFilters: ActiveFilter[]) {
  const params: Record<string, string> = {}
  const filtrosAdicionais: string[] = []

  for (const f of activeFilters) {
    if (f.field === 'data') {
      if (f.operator === 'gte') params.dataInicio = f.value
      else if (f.operator === 'lte') params.dataFim = f.value
      else if (f.operator === 'eq') {
        params.dataInicio = f.value
        params.dataFim = f.value
      } else if (f.operator === 'gt' || f.operator === 'lt' || f.operator === 'neq') {
        // Operadores sem mapeamento direto em dataInicio/dataFim: usa filtro adicional.
        filtrosAdicionais.push(
          `data:${f.operator}:${encodeURIComponent(f.value)}`,
        )
      }
    } else if (CAMPOS_FILTRO_ADICIONAL.has(f.field)) {
      filtrosAdicionais.push(
        `${f.field}:${f.operator}:${encodeURIComponent(f.value)}`,
      )
    } else {
      // tipo, status, contaId -- enum direto.
      params[f.field] = f.value
    }
  }

  if (filtrosAdicionais.length > 0) {
    params.filtros = filtrosAdicionais.join(',')
  }
  return params
}

function badgeVariant(tipo: string) {
  if (tipo === 'RECEITA') return 'default' as const
  if (tipo === 'DESPESA') return 'destructive' as const
  return 'secondary' as const
}

const COLUMNS_BASE: ColumnDef<Transacao>[] = [
  { key: 'descricao', label: 'Descricao', sortable: true, className: 'font-medium' },
  {
    key: 'tipo',
    label: 'Tipo',
    render: (value) => (
      <Badge variant={badgeVariant(String(value))}>
        {formatTipoTransacao(String(value))}
      </Badge>
    ),
  },
  {
    key: 'status',
    label: 'Status',
    render: (value) => (
      <StatusBadge
        status={String(value)}
        config={STATUS_TRANSACAO_CONFIG}
        fallbackLabel={String(value)}
      />
    ),
  },
  {
    key: 'data',
    label: 'Data',
    sortable: true,
    render: (value) => formatDate(String(value)),
  },
  {
    key: 'valor',
    label: 'Valor',
    sortable: true,
    className: 'text-right tabular-nums',
    render: (value) => formatBRL(Number(value)),
  },
]

export default function TransacoesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [selecionada, setSelecionada] = useState<Transacao | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const deleteMutation = useMutation({
    mutationFn: (id: string) => transacoesService.deletar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transacoes'] })
      setConfirmDeleteId(null)
    },
  })

  const { data: contas } = useQuery({
    queryKey: ['contas'],
    queryFn: () => contasService.listar(),
  })
  const contasMap = useMemo(
    () => new Map((contas ?? []).map((c) => [c.id, c.nome])),
    [contas],
  )
  const filterFields = useMemo<FilterFieldDef[]>(
    () => [
      ...FILTER_FIELDS_BASE,
      {
        name: 'contaId',
        label: 'Conta',
        type: 'enum',
        options: (contas ?? []).map((c) => ({ value: c.id, label: c.nome })),
      },
    ],
    [contas],
  )
  const columns = useMemo<ColumnDef<Transacao>[]>(
    () => [
      ...COLUMNS_BASE,
      {
        key: 'contaId',
        label: 'Conta',
        render: (value) => contasMap.get(String(value)) ?? String(value),
      },
    ],
    [contasMap],
  )

  const {
    data,
    totalElements,
    totalPages,
    page,
    sort,
    isLoading,
    isError,
    activeFilters,
    addFilter,
    removeFilter,
    clearFilters,
    setPage,
    setSort,
  } = useListPage<Transacao, Record<string, string>>({
    queryKey: 'transacoes',
    fetcher: ({ activeFilters, page, size, sort }) =>
      transacoesService.listar({
        ...buildBackendParams(activeFilters),
        page,
        size,
        sort,
      }),
    defaultSort: { field: 'data', dir: 'desc' },
  })

  // Reconstroi label/displayValue/operatorLabel dos chips (a URL guarda so o
  // nome do campo, o operador e o valor cru).
  const chips = activeFilters.map((f) => ({
    ...f,
    label: filterFields.find((d) => d.name === f.field)?.label ?? f.label,
    displayValue: resolveDisplay(f.field, f.value, filterFields),
    operatorLabel: resolveOperatorLabel(f.field, f.operator, filterFields),
  }))

  const handleExport = () => {
    exportToCsv(
      'transacoes',
      data.map((t) => ({
        descricao: t.descricao,
        tipo: formatTipoTransacao(t.tipo),
        status: STATUS_TRANSACAO_CONFIG[t.status]?.label ?? t.status,
        data: t.data,
        valor: t.valor,
        conta: contasMap.get(t.contaId) ?? t.contaId,
      })),
      [
        { key: 'descricao', label: 'Descricao' },
        { key: 'tipo', label: 'Tipo' },
        { key: 'status', label: 'Status' },
        { key: 'data', label: 'Data' },
        { key: 'valor', label: 'Valor' },
        { key: 'conta', label: 'Conta' },
      ],
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">Transacoes</h1>
        <div className="flex items-center gap-2">
          <ActionsPanel
            entityType="transacao"
            entityId={selecionada?.id ?? null}
            entityLabel={selecionada?.descricao}
            screenCode={SCREEN_CODE}
            onExportCsv={data.length > 0 ? handleExport : undefined}
          />
          <Button onClick={() => router.push('/transacoes/novo')}>Nova Transacao</Button>
        </div>
      </div>

      <FilterBar
        fields={filterFields}
        activeFilters={chips}
        onAdd={addFilter}
        onRemove={removeFilter}
        onClear={clearFilters}
      />

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar transacoes.</p>
      )}

      {!isError && (
        <Card>
          <CardContent className="p-0">
            <DataTable
              data={data}
              columns={columns}
              keyField="id"
              isLoading={isLoading}
              emptyMessage="Nenhuma transacao cadastrada."
              sort={sort}
              onSortChange={setSort}
              onRowClick={setSelecionada}
              rowActions={(row) =>
                confirmDeleteId === row.id ? (
                  <span className="inline-flex gap-2">
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() => deleteMutation.mutate(row.id)}
                      disabled={deleteMutation.isPending}
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
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Editar ${row.descricao}`}
                      onClick={() =>
                        router.push(`/transacoes/${row.id}/editar`)
                      }
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Historico de ${row.descricao}`}
                      onClick={() => setSelecionada(row)}
                    >
                      <History className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Excluir ${row.descricao}`}
                      onClick={() => setConfirmDeleteId(row.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                )
              }
            />
          </CardContent>
        </Card>
      )}

      {!isError && totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            {totalElements} transacao(oes)
            {selecionada && ` -- selecionada: ${selecionada.descricao}`}
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page <= 0}
              onClick={() => setPage(page - 1)}
            >
              Anterior
            </Button>
            <span>
              Pagina {page + 1} de {Math.max(totalPages, 1)}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage(page + 1)}
            >
              Proxima
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
