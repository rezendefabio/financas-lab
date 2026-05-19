'use client'

/**
 * FormGrid -- wrapper de layout que configura um CSS Grid de 12 colunas
 * (form-kit, fase UI-7).
 *
 * Usado em conjunto com `FormCol` para posicionar campos de formulario num
 * grid responsivo. Componente puro de layout: sem estado, sem logica de
 * negocio.
 */

import { cn } from '@/shared/lib/utils'

interface FormGridProps {
  children: React.ReactNode
  className?: string
}

export function FormGrid({ children, className }: FormGridProps) {
  return (
    <div className={cn('grid grid-cols-12 gap-4', className)}>{children}</div>
  )
}
