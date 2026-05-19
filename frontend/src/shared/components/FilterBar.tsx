'use client'

/**
 * FilterBar -- barra de filtros com chips empilhaveis (ADR-014, fase UI-5).
 *
 * Exibe um botao "+ Filtro" que abre um `<Popover>` em tres passos (escolher
 * campo, escolher operador, preencher valor) e renderiza um chip `<Badge>` por
 * filtro ativo, com botao de remocao individual e um botao "Limpar tudo".
 *
 * Cada filtro carrega um operador adequado ao tipo do campo (string, number,
 * date, boolean, enum). Para campos `boolean` o operador ja e o valor, entao o
 * passo 3 e omitido.
 *
 * Componente puro de apresentacao: nao conhece tipos de dominio. O estado dos
 * filtros vive no consumidor (tipicamente o hook `useListPage`).
 */

import * as React from 'react'
import { ListFilter, X } from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/shared/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

/** Tipos de campo suportados pela barra de filtros. */
export type FilterFieldType = 'string' | 'number' | 'date' | 'boolean' | 'enum'

/** Definicao de um campo filtravel. */
export interface FilterFieldDef {
  name: string
  label: string
  type: FilterFieldType
  /** Opcoes para `type='enum'`. */
  options?: { value: string; label: string }[]
}

/** Operador de filtro: valor serializado + rotulo humano. */
export type FilterOperator = {
  value: string
  label: string
}

/**
 * Operadores disponiveis por tipo de campo. Para `boolean` o operador e o
 * proprio valor (`true`/`false`), entao o passo de valor e omitido.
 */
export const OPERATORS_BY_TYPE: Record<FilterFieldType, FilterOperator[]> = {
  string: [
    { value: 'contains', label: 'contem' },
    { value: 'not_contains', label: 'nao contem' },
    { value: 'eq', label: 'igual a' },
    { value: 'neq', label: 'diferente de' },
  ],
  number: [
    { value: 'eq', label: '= igual' },
    { value: 'neq', label: '!= diferente' },
    { value: 'gt', label: '> maior que' },
    { value: 'gte', label: '>= maior ou igual' },
    { value: 'lt', label: '< menor que' },
    { value: 'lte', label: '<= menor ou igual' },
  ],
  date: [
    { value: 'eq', label: '= nesse dia' },
    { value: 'neq', label: '!= diferente de' },
    { value: 'gt', label: '> depois de' },
    { value: 'gte', label: '>= a partir de' },
    { value: 'lt', label: '< antes de' },
    { value: 'lte', label: '<= ate' },
  ],
  boolean: [
    { value: 'true', label: 'verdadeiro' },
    { value: 'false', label: 'falso' },
  ],
  enum: [
    { value: 'eq', label: 'igual a' },
    { value: 'neq', label: 'diferente de' },
  ],
}

/** Filtro ativo, ja serializado. */
export interface ActiveFilter {
  field: string
  /** Operador serializado (ex: `contains`, `gte`, `true`). */
  operator: string
  /** Label humano do campo. */
  label: string
  /** Valor serializado como string (vai para a URL). Vazio para `boolean`. */
  value: string
  /** Valor para exibir no chip (ex: "Receita" em vez de "RECEITA"). */
  displayValue: string
  /** Label humano do operador (ex: "contem", ">="). */
  operatorLabel: string
}

export interface FilterBarProps {
  fields: FilterFieldDef[]
  activeFilters: ActiveFilter[]
  onAdd: (filter: ActiveFilter) => void
  onRemove: (fieldName: string) => void
  onClear: () => void
}

/** Converte um valor cru no `displayValue` legivel conforme o tipo do campo. */
function toDisplayValue(field: FilterFieldDef, value: string): string {
  if (field.type === 'enum') {
    return field.options?.find((o) => o.value === value)?.label ?? value
  }
  return value
}

/** `true` quando o tipo dispensa o passo de valor (operador ja e o valor). */
function operatorIsValue(type: FilterFieldType): boolean {
  return type === 'boolean'
}

export function FilterBar({
  fields,
  activeFilters,
  onAdd,
  onRemove,
  onClear,
}: FilterBarProps) {
  const [open, setOpen] = React.useState(false)
  const [selectedField, setSelectedField] = React.useState<string>('')
  const [selectedOperator, setSelectedOperator] = React.useState<string>('')
  const [draftValue, setDraftValue] = React.useState<string>('')

  const field = fields.find((f) => f.name === selectedField)
  const operators = field ? OPERATORS_BY_TYPE[field.type] : []
  const operator = operators.find((o) => o.value === selectedOperator)
  const valueless = field ? operatorIsValue(field.type) : false

  const resetDraft = () => {
    setSelectedField('')
    setSelectedOperator('')
    setDraftValue('')
  }

  const handleOpenChange = (next: boolean) => {
    setOpen(next)
    if (!next) resetDraft()
  }

  const canApply = Boolean(field && operator && (valueless || draftValue !== ''))

  const handleApply = () => {
    if (!field || !operator) return
    if (!valueless && draftValue === '') return
    const value = valueless ? '' : draftValue
    onAdd({
      field: field.name,
      operator: operator.value,
      label: field.label,
      value,
      displayValue: valueless ? operator.label : toDisplayValue(field, value),
      operatorLabel: operator.label,
    })
    resetDraft()
    setOpen(false)
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Popover open={open} onOpenChange={handleOpenChange}>
        <PopoverTrigger
          render={
            <Button type="button" variant="outline" size="sm">
              <ListFilter className="h-3.5 w-3.5" />
              Filtro
            </Button>
          }
        />
        <PopoverContent align="start" className="w-72">
          <div className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-medium text-muted-foreground">
                Campo
              </label>
              <Select
                value={selectedField}
                onValueChange={(v) => {
                  setSelectedField(v ?? '')
                  setSelectedOperator('')
                  setDraftValue('')
                }}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Selecione o campo">
                    {(v: string | null) =>
                      fields.find((f) => f.name === v)?.label ?? 'Selecione o campo'
                    }
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {fields.map((f) => (
                    <SelectItem key={f.name} value={f.name}>
                      {f.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {field && (
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-medium text-muted-foreground">
                  Operador
                </label>
                <Select
                  value={selectedOperator}
                  onValueChange={(v) => {
                    setSelectedOperator(v ?? '')
                    setDraftValue('')
                  }}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Selecione o operador">
                      {(v: string | null) =>
                        operators.find((o) => o.value === v)?.label ??
                        'Selecione o operador'
                      }
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {operators.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {field && operator && !valueless && (
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-medium text-muted-foreground">
                  Valor
                </label>
                {field.type === 'string' && (
                  <Input
                    type="text"
                    value={draftValue}
                    onChange={(e) => setDraftValue(e.target.value)}
                  />
                )}
                {field.type === 'number' && (
                  <Input
                    type="number"
                    value={draftValue}
                    onChange={(e) => setDraftValue(e.target.value)}
                  />
                )}
                {field.type === 'date' && (
                  <Input
                    type="date"
                    value={draftValue}
                    onChange={(e) => setDraftValue(e.target.value)}
                  />
                )}
                {field.type === 'enum' && (
                  <Select
                    value={draftValue}
                    onValueChange={(v) => setDraftValue(v ?? '')}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Selecione">
                        {(v: string | null) =>
                          field.options?.find((o) => o.value === v)?.label ??
                          'Selecione'
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(field.options ?? []).map((o) => (
                        <SelectItem key={o.value} value={o.value}>
                          {o.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              </div>
            )}

            <Button
              type="button"
              size="sm"
              disabled={!canApply}
              onClick={handleApply}
            >
              Aplicar
            </Button>
          </div>
        </PopoverContent>
      </Popover>

      {activeFilters.map((filter) => (
        <Badge key={filter.field} variant="secondary" className="gap-1">
          {filter.label} {filter.operatorLabel}
          {filter.value !== '' && <>: {filter.displayValue}</>}
          <button
            type="button"
            aria-label={`Remover filtro ${filter.label}`}
            onClick={() => onRemove(filter.field)}
          >
            <X className="ml-1 h-3 w-3 cursor-pointer" />
          </button>
        </Badge>
      ))}

      {activeFilters.length >= 1 && (
        <Button type="button" variant="ghost" size="sm" onClick={onClear}>
          Limpar tudo
        </Button>
      )}
    </div>
  )
}
