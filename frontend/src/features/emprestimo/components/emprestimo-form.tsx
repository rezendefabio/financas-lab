'use client'

import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
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
import {
  emprestimoFormSchema,
  TIPO_EMPRESTIMO_OPTIONS,
  type EmprestimoFormValues,
} from '../types/emprestimo'

const QUITADO_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'false', label: 'Em aberto' },
  { value: 'true', label: 'Quitado' },
]

interface EmprestimoFormProps {
  defaultValues: EmprestimoFormValues
  onSubmit: (values: EmprestimoFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
}

export function EmprestimoForm({
  defaultValues,
  onSubmit,
  isSubmitting,
  apiError,
  onClearApiError,
  submitLabel,
  onCancel,
}: EmprestimoFormProps) {
  const form = useForm<EmprestimoFormValues>({
    resolver: zodResolver(emprestimoFormSchema),
    defaultValues,
  })
  const { clearDraft } = useDraftForm(form)

  const handleSubmit = form.handleSubmit((values) => {
    clearDraft()
    onClearApiError()
    onSubmit(values)
  })

  const handleCancel = () => {
    clearDraft()
    onCancel()
  }

  return (
    <Form {...form}>
      <form onSubmit={handleSubmit} className="space-y-4">
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
                  <FormLabel>Nome do terceiro</FormLabel>
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
                      render={({ field: f }) => (
                        <Select value={f.value} onValueChange={f.onChange}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Selecione">
                              {(v: string | null) =>
                                TIPO_EMPRESTIMO_OPTIONS.find((o) => o.value === v)
                                  ?.label ?? 'Selecione'
                              }
                            </SelectValue>
                          </SelectTrigger>
                          <SelectContent>
                            {TIPO_EMPRESTIMO_OPTIONS.map((opt) => (
                              <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormControl>
                  <FormMessage />
                  {/* manter `field` referenciado para evitar warning de unused */}
                  <input type="hidden" value={field.value} readOnly />
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
              name="quitado"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Status</FormLabel>
                  <FormControl>
                    <Controller
                      control={form.control}
                      name="quitado"
                      render={({ field: f }) => (
                        <Select
                          value={String(f.value)}
                          onValueChange={(v) => f.onChange(v === 'true')}
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
                            {QUITADO_OPTIONS.map((opt) => (
                              <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormControl>
                  <FormMessage />
                  <input
                    type="hidden"
                    value={String(field.value)}
                    readOnly
                  />
                </FormItem>
              )}
            />
          </FormCol>
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
