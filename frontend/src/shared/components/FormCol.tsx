'use client'

/**
 * FormCol -- wrapper de layout que define o span de colunas dentro de um
 * `FormGrid` de 12 colunas (form-kit, fase UI-7).
 *
 * As classes de span sao strings literais completas (`col-span-6`, nao
 * `col-span-${n}`) para que o Tailwind JIT as inclua no build.
 *
 * Componente puro de layout: sem estado, sem logica de negocio.
 */

import { cn } from '@/shared/lib/utils'

type ColSpan = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12

interface FormColProps {
  children: React.ReactNode
  span?: ColSpan
  className?: string
}

const SPAN_CLASS: Record<ColSpan, string> = {
  1: 'col-span-1',
  2: 'col-span-2',
  3: 'col-span-3',
  4: 'col-span-4',
  5: 'col-span-5',
  6: 'col-span-6',
  7: 'col-span-7',
  8: 'col-span-8',
  9: 'col-span-9',
  10: 'col-span-10',
  11: 'col-span-11',
  12: 'col-span-12',
}

export function FormCol({ children, span = 12, className }: FormColProps) {
  return (
    <div className={cn(SPAN_CLASS[span], className)}>{children}</div>
  )
}
