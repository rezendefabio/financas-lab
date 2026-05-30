'use client'

import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import type { Prioridade } from '../types/lembrete'

const PRIORIDADES: { value: Prioridade; label: string }[] = [
  { value: 'BAIXA', label: 'Baixa' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'ALTA', label: 'Alta' },
]

export const lembreteFormSchema = z.object({
  titulo: z.string().min(1, 'Titulo obrigatorio').max(100),
  descricao: z.string().max(500).optional().or(z.literal('')),
  dataLembrete: z.string().min(1, 'Data obrigatoria'),
  prioridade: z.enum(['BAIXA', 'MEDIA', 'ALTA']),
  concluido: z.boolean(),
})

export type LembreteFormValues = z.infer<typeof lembreteFormSchema>

export function defaultLembreteFormValues(): LembreteFormValues {
  return {
    titulo: '',
    descricao: '',
    dataLembrete: '',
    prioridade: 'MEDIA',
    concluido: false,
  }
}

interface LembreteFormProps {
  defaultValues: LembreteFormValues
  onSubmit: (values: LembreteFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
  /** Quando false, esconde o campo "concluido" (criacao). */
  showConcluido?: boolean
}

export function LembreteForm({
  defaultValues,
  onSubmit,
  isSubmitting,
  apiError,
  onClearApiError,
  submitLabel,
  onCancel,
  showConcluido = true,
}: LembreteFormProps) {
  const form = useForm<LembreteFormValues>({
    resolver: zodResolver(lembreteFormSchema),
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
        onSubmit={form.handleSubmit((values) => {
          clearDraft()
          onClearApiError()
          onSubmit(values)
        })}
        className="space-y-4"
      >
        <FormGrid>
          <FormCol span={12}>
            <FormField
              control={form.control}
              name="titulo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Titulo</FormLabel>
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
              name="descricao"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descricao</FormLabel>
                  <FormControl>
                    <textarea
                      className="flex min-h-20 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                      maxLength={500}
                      rows={3}
                      {...field}
                      value={field.value ?? ''}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={5}>
            <FormField
              control={form.control}
              name="dataLembrete"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Data</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          <FormCol span={4}>
            <FormItem>
              <FormLabel>Prioridade</FormLabel>
              <Controller
                control={form.control}
                name="prioridade"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Selecione">
                        {(v: string | null) =>
                          PRIORIDADES.find((p) => p.value === v)?.label ?? 'Selecione'
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {PRIORIDADES.map((p) => (
                        <SelectItem key={p.value} value={p.value}>
                          {p.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              <FormMessage />
            </FormItem>
          </FormCol>

          {showConcluido && (
            <FormCol span={3}>
              <FormItem>
                <FormLabel>Concluido</FormLabel>
                <FormControl>
                  <Controller
                    control={form.control}
                    name="concluido"
                    render={({ field }) => (
                      <input
                        type="checkbox"
                        className="h-5 w-5 rounded border border-input"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    )}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            </FormCol>
          )}
        </FormGrid>

        {apiError && <p className="text-sm text-destructive">{apiError}</p>}

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
