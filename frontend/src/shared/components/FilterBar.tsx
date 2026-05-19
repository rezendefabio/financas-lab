'use client'

/**
 * FilterBar -- barra de filtros com chips empilhaveis (ADR-014, fase UI-5).
 *
 * Exibe um botao "+ Filtro" que abre um `<Popover>` em dois passos (escolher
 * campo, preencher valor) e renderiza um chip `<Badge>` por filtro ativo, com
 * botao de remocao individual e um botao "Limpar tudo".
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

/** Filtro ativo, ja serializado. */
export interface ActiveFilter {
  field: string
  /** Label humano do campo. */
  label: string
  /** Valor serializado como string (vai para a URL). */
  value: string
  /** Valor para exibir no chip (ex: "Receita" em vez de "RECEITA"). */
  displayValue: string
}

export interface FilterBarProps {
  fields: FilterFieldDef[]
  activeFilters: ActiveFilter[]
  onAdd: (filter: ActiveFilter) => void
  onRemove: (fieldName: string) => void
  onClear: () => void
}

const BOOLEAN_OPTIONS = [
  { value: 'true', label: 'Sim' },
  { value: 'false', label: 'Nao' },
]

/** Converte um valor cru no `displayValue` legivel conforme o tipo do campo. */
function toDisplayValue(field: FilterFieldDef, value: string): string {
  if (field.type === 'boolean') {
    return BOOLEAN_OPTIONS.find((o) => o.value === value)?.label ?? value
  }
  if (field.type === 'enum') {
    return field.options?.find((o) => o.value === value)?.label ?? value
  }
  return value
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
  const [draftValue, setDraftValue] = React.useState<string>('')

  const field = fields.find((f) => f.name === selectedField)

  const resetDraft = () => {
    setSelectedField('')
    setDraftValue('')
  }

  const handleOpenChange = (next: boolean) => {
    setOpen(next)
    if (!next) resetDraft()
  }

  const handleApply = () => {
    if (!field || draftValue === '') return
    onAdd({
      field: field.name,
      label: field.label,
      value: draftValue,
      displayValue: toDisplayValue(field, draftValue),
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
                {field.type === 'boolean' && (
                  <Select
                    value={draftValue}
                    onValueChange={(v) => setDraftValue(v ?? '')}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Selecione">
                        {(v: string | null) =>
                          BOOLEAN_OPTIONS.find((o) => o.value === v)?.label ??
                          'Selecione'
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {BOOLEAN_OPTIONS.map((o) => (
                        <SelectItem key={o.value} value={o.value}>
                          {o.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
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
              disabled={!field || draftValue === ''}
              onClick={handleApply}
            >
              Aplicar
            </Button>
          </div>
        </PopoverContent>
      </Popover>

      {activeFilters.map((filter) => (
        <Badge key={filter.field} variant="secondary" className="gap-1">
          {filter.label}: {filter.displayValue}
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
