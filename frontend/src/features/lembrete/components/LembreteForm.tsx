'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useEffect } from 'react'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import { PRIORIDADE_LEMBRETE_OPTIONS } from '../types/lembrete'
import type { PrioridadeLembrete } from '../types/lembrete'
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
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'

const schema = z.object({
  titulo: z.string().min(1, 'Titulo obrigatorio').max(100, 'Maximo 100 caracteres'),
  descricao: z.string().max(500, 'Maximo 500 caracteres').optional(),
  dataLembrete: z.string().min(1, 'Data obrigatoria'),
  prioridade: z.enum(['BAIXA', 'MEDIA', 'ALTA'], {
    message: 'Selecione uma prioridade',
  }),
  concluido: z.boolean(),
})

export type LembreteFormValues = z.infer<typeof schema>

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
  defaultValues?: LembreteFormValues
  loadedValues?: LembreteFormValues | null
  onSubmit: (values: LembreteFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
}

export function LembreteForm({
  defaultValues,
  loadedValues,
  onSubmit,
  isSubmitting,
  apiError,
  onClearApiError,
  submitLabel,
  onCancel,
}: LembreteFormProps) {
  const form = useForm<LembreteFormValues>({
    resolver: zodResolver(schema) as Resolver<LembreteFormValues>,
    defaultValues: defaultValues ?? defaultLembreteFormValues(),
  })
  const { clearDraft, resetWithDraft } = useDraftForm(form)

  useEffect(() => {
    if (loadedValues) {
      resetWithDraft(loadedValues)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadedValues])

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => {
          onClearApiError()
          clearDraft()
          onSubmit(v)
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
                    <Input
                      className="w-full"
                      placeholder="Ex: Pagar boleto"
                      maxLength={100}
                      {...field}
                    />
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
                      className="w-full min-h-[80px] rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                      placeholder="Detalhes (opcional)"
                      maxLength={500}
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      name={field.name}
                      ref={field.ref}
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
                    <Input type="date" className="w-full" {...field} />
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
                  <Select
                    value={field.value}
                    onValueChange={(v) => field.onChange(v as PrioridadeLembrete)}
                  >
                    <SelectTrigger className="w-full" aria-label="Prioridade">
                      <SelectValue placeholder="Selecione a prioridade">
                        {(v: string | null) =>
                          PRIORIDADE_LEMBRETE_OPTIONS.find((o) => o.value === v)?.label ??
                          'Selecione a prioridade'
                        }
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {PRIORIDADE_LEMBRETE_OPTIONS.map((o) => (
                        <SelectItem key={o.value} value={o.value}>
                          {o.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {form.formState.errors.prioridade && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.prioridade.message}
                </p>
              )}
            </FormItem>
          </FormCol>

          <FormCol span={3}>
            <FormItem>
              <FormLabel>Status</FormLabel>
              <Controller
                control={form.control}
                name="concluido"
                render={({ field }) => (
                  <label className="flex items-center gap-2 cursor-pointer select-none text-sm pt-2">
                    <input
                      type="checkbox"
                      checked={field.value}
                      onChange={(e) => field.onChange(e.target.checked)}
                      aria-label="Concluido"
                    />
                    <span>Concluido</span>
                  </label>
                )}
              />
            </FormItem>
          </FormCol>
        </FormGrid>

        {apiError && <p className="text-sm text-destructive">{apiError}</p>}

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              clearDraft()
              onCancel()
            }}
          >
            Cancelar
          </Button>
        </div>
      </form>
    </Form>
  )
}
