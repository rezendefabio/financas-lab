'use client'

/**
 * LookupField -- combobox filtravel para campos de FK (form-kit, fase UI-7).
 *
 * Carrega opcoes via `useQuery` e filtra client-side pelo texto digitado.
 * Substitui o padrao de `useQuery` + `Select` estatico, adicionando busca
 * inline sem nova dependencia (nao usa `Command`/`cmdk`).
 *
 * Completamente generico: nao conhece dominios especificos -- recebe
 * `queryKey` e `queryFn` como props.
 */

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Check, ChevronsUpDown, X } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/shared/components/ui/popover'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'

/** Opcao exibida pelo combobox: valor serializado + rotulo humano. */
export interface LookupOption {
  value: string
  label: string
}

export interface LookupFieldProps {
  value: string | null
  onChange: (value: string | null) => void
  queryKey: string[]
  queryFn: () => Promise<LookupOption[]>
  placeholder?: string
  emptyMessage?: string
  disabled?: boolean
  className?: string
}

export function LookupField({
  value,
  onChange,
  queryKey,
  queryFn,
  placeholder = 'Selecione...',
  emptyMessage = 'Nenhuma opcao encontrada.',
  disabled,
  className,
}: LookupFieldProps) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')

  const { data: options = [], isLoading } = useQuery({
    queryKey,
    queryFn,
    staleTime: 5 * 60 * 1000,
  })

  const filtered = options.filter((o) =>
    (o.label ?? '').toLowerCase().includes(search.toLowerCase()),
  )

  const selected = options.find((o) => o.value === value) ?? null

  function handleSelect(optionValue: string) {
    onChange(optionValue)
    setOpen(false)
    setSearch('')
  }

  function handleClear() {
    onChange(null)
    setOpen(false)
    setSearch('')
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            type="button"
            variant="outline"
            disabled={disabled}
            className={cn(
              'w-full justify-between font-normal',
              !selected && 'text-muted-foreground',
              className,
            )}
          >
            {selected ? selected.label : placeholder}
            <ChevronsUpDown className="h-4 w-4 shrink-0 opacity-50" />
          </Button>
        }
      />
      <PopoverContent align="start" className="w-(--anchor-width) p-0">
        <div className="border-b p-2">
          <Input
            placeholder="Buscar..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />
        </div>
        <div className="max-h-60 overflow-y-auto">
          {isLoading ? (
            <div className="space-y-1 p-2">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
          ) : filtered.length === 0 ? (
            <p className="p-4 text-center text-sm text-muted-foreground">
              {emptyMessage}
            </p>
          ) : (
            filtered.map((option) => (
              <button
                key={option.value}
                type="button"
                className={cn(
                  'flex w-full cursor-pointer items-center gap-2 px-3 py-2 text-sm hover:bg-accent',
                  value === option.value && 'font-medium',
                )}
                onClick={() => handleSelect(option.value)}
              >
                <Check
                  className={cn(
                    'h-4 w-4 shrink-0',
                    value === option.value ? 'opacity-100' : 'opacity-0',
                  )}
                />
                {option.label}
              </button>
            ))
          )}
        </div>
        {value !== null && (
          <div className="border-t p-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="w-full text-muted-foreground"
              onClick={handleClear}
            >
              <X className="mr-2 h-4 w-4" />
              Limpar selecao
            </Button>
          </div>
        )}
      </PopoverContent>
    </Popover>
  )
}
