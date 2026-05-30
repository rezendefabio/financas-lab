'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { MoneyInput } from '@/shared/components/MoneyInput'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import type { TipoEmprestimo } from '../types'

export const emprestimoFormSchema = z.object({
  descricao: z.string().min(1, 'Descricao obrigatoria').max(100),
  nomeTerceiro: z.string().max(100).optional().or(z.literal('')),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3),
  dataEmprestimo: z.string().min(1, 'Data obrigatoria'),
  quitado: z.boolean(),
})

export type EmprestimoFormValues = z.infer<typeof emprestimoFormSchema>

export function defaultEmprestimoFormValues(): EmprestimoFormValues {
  return {
    descricao: '',
    nomeTerceiro: '',
    tipo: 'CONCEDIDO',
    valor: 0,
    moeda: 'BRL',
    dataEmprestimo: new Date().toISOString().slice(0, 10),
    quitado: false,
  }
}

const TIPOS: { value: TipoEmprestimo; label: string }[] = [
  { value: 'CONCEDIDO', label: 'Concedido' },
  { value: 'RECEBIDO', label: 'Recebido' },
]

const QUITADO_OPTIONS: { value: 'true' | 'false'; label: string }[] = [
  { value: 'false', label: 'Em aberto' },
  { value: 'true', label: 'Quitado' },
]

export interface EmprestimoFormProps {
  defaultValues: EmprestimoFormValues
  onSubmit: (values: EmprestimoFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
  showQuitado?: boolean
}

export function EmprestimoForm({
  defaultValues,
  onSubmit,
  isSubmitting,
  apiError,
  onClearApiError,
  submitLabel,
  onCancel,
  showQuitado = false,
}: EmprestimoFormProps) {
  const form = useForm<EmprestimoFormValues>({
    resolver: zodResolver(emprestimoFormSchema),
    defaultValues,
  })
  const { clearDraft } = useDraftForm(form)

  const handleCancel = () => {
    clearDraft()
    onCancel()
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => {
          clearDraft()
          onClearApiError()
          onSubmit(v)
        })}
        className="space-y-4"
      >
        <FormGrid>
          <FormCol span={12}>
            <FormField
              control={form.control}
              name="descricao"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descricao</FormLabel>
                  <FormControl>
                    <Input maxLength={100} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={12}>
            <FormField
              control={form.control}
              name="nomeTerceiro"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nome do terceiro (opcional)</FormLabel>
                  <FormControl>
                    <Input
                      maxLength={100}
                      {...field}
                      value={field.value ?? ''}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={6}>
            <FormField
              control={form.control}
              name="tipo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <FormControl>
                    <Controller
                      control={form.control}
                      name="tipo"
                      render={({ field: ctrl }) => (
                        <Select
                          value={ctrl.value}
                          onValueChange={(v) =>
                            ctrl.onChange(v as TipoEmprestimo)
                          }
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Selecione">
                              {(v: string | null) =>
                                TIPOS.find((t) => t.value === v)?.label ??
                                'Selecione'
                              }
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {TIPOS.map((t) => (
                              <SelectItem key={t.value} value={t.value}>
                                {t.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={6}>
            <FormField
              control={form.control}
              name="dataEmprestimo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Data do emprestimo</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={6}>
            <FormField
              control={form.control}
              name="valor"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Valor (R$)</FormLabel>
                  <FormControl>
                    <MoneyInput
                      value={field.value}
                      onChange={field.onChange}
                      id={field.name}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          {showQuitado && (
            <FormCol span={6}>
              <FormField
                control={form.control}
                name="quitado"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Situacao</FormLabel>
                    <FormControl>
                      <Controller
                        control={form.control}
                        name="quitado"
                        render={({ field: ctrl }) => (
                          <Select
                            value={ctrl.value ? 'true' : 'false'}
                            onValueChange={(v) => ctrl.onChange(v === 'true')}
                          >
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Selecione">
                                {(v: string | null) =>
                                  QUITADO_OPTIONS.find((o) => o.value === v)
                                    ?.label ?? 'Selecione'
                                }
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {QUITADO_OPTIONS.map((o) => (
                                <SelectItem key={o.value} value={o.value}>
                                  {o.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormCol>
          )}
        </FormGrid>

        <input type="hidden" {...form.register('moeda')} />

        {apiError && (
          <p className="text-sm text-destructive" role="alert">
            {apiError}
          </p>
        )}

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
          <Button type="button" variant="outline" onClick={handleCancel}>
            Cancelar
          </Button>
        </div>
      </form>
    </Form>
  )
}
