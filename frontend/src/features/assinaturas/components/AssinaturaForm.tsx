'use client'
import { Controller, useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from '@/shared/components/ui/form'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/shared/components/ui/select'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { MoneyInput } from '@/shared/components/MoneyInput'

// Espelhamento Java <-> Zod (B6) de CriarAssinaturaRequest.java:
// @NotBlank @Size(max=100) String nome   -> z.string().min(1).max(100)
// @NotNull TipoAssinatura tipo            -> z.enum([...])
// @NotNull BigDecimal valorMensal (>0)    -> z.number().positive()
// @NotNull @Size(min=3,max=3) String moeda-> z.string() (campo oculto fixo 'BRL')
// @NotNull LocalDate dataRenovacao         -> z.string().min(1)
// boolean ativa                            -> z.boolean()
export const AssinaturaFormSchema = z.object({
  nome: z.string().min(1, 'Obrigatorio').max(100, 'Maximo 100 caracteres'),
  tipo: z.enum(['STREAMING', 'SOFTWARE', 'ACADEMIA', 'OUTROS']),
  valorMensal: z.number().positive('Deve ser positivo'),
  moeda: z.string(),
  dataRenovacao: z.string().min(1, 'Obrigatorio'),
  ativa: z.boolean(),
})

export type AssinaturaFormValues = z.infer<typeof AssinaturaFormSchema>

const TIPOS: { value: AssinaturaFormValues['tipo']; label: string }[] = [
  { value: 'STREAMING', label: 'Streaming' },
  { value: 'SOFTWARE', label: 'Software' },
  { value: 'ACADEMIA', label: 'Academia' },
  { value: 'OUTROS', label: 'Outros' },
]

export function defaultAssinaturaFormValues(): AssinaturaFormValues {
  return {
    nome: '',
    tipo: 'STREAMING',
    valorMensal: 0,
    moeda: 'BRL',
    dataRenovacao: '',
    ativa: true,
  }
}

interface AssinaturaFormProps {
  defaultValues: AssinaturaFormValues
  onSubmit: (values: AssinaturaFormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
}

export function AssinaturaForm({
  defaultValues, onSubmit, isSubmitting, apiError,
  onClearApiError, submitLabel, onCancel,
}: AssinaturaFormProps) {
  const form = useForm<AssinaturaFormValues>({
    resolver: zodResolver(AssinaturaFormSchema) as Resolver<AssinaturaFormValues>,
    defaultValues,
  })
  const { clearDraft } = useDraftForm(form)

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => { clearDraft(); onClearApiError(); onSubmit(v) })}
        className="space-y-4"
      >
        <FormGrid>
          <FormCol span={12}>
            <FormField control={form.control} name="nome" render={({ field }) => (
              <FormItem>
                <FormLabel>Nome do servico</FormLabel>
                <FormControl>
                  <Input maxLength={100} placeholder="Ex: Netflix" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
          </FormCol>

          <FormCol span={6}>
            <FormItem>
              <FormLabel>Tipo</FormLabel>
              <Controller control={form.control} name="tipo" render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Selecione">
                      {(v: string | null) => TIPOS.find((t) => t.value === v)?.label ?? 'Selecione'}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {TIPOS.map((t) => (
                      <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )} />
            </FormItem>
          </FormCol>

          <FormCol span={6}>
            <FormField control={form.control} name="valorMensal" render={({ field }) => (
              <FormItem>
                <FormLabel>Valor mensal (R$)</FormLabel>
                <FormControl>
                  <MoneyInput value={field.value} onChange={field.onChange} id={field.name} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
          </FormCol>

          <FormCol span={6}>
            <FormField control={form.control} name="dataRenovacao" render={({ field }) => (
              <FormItem>
                <FormLabel>Data de renovacao</FormLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )} />
          </FormCol>

          <FormCol span={6}>
            <Controller control={form.control} name="ativa" render={({ field }) => (
              <FormItem>
                <FormLabel htmlFor="assinatura-ativa">Ativa</FormLabel>
                <div className="flex h-10 items-center">
                  <input
                    id="assinatura-ativa"
                    type="checkbox"
                    className="h-4 w-4 accent-primary"
                    checked={field.value}
                    onChange={(e) => field.onChange(e.target.checked)}
                  />
                </div>
              </FormItem>
            )} />
          </FormCol>
        </FormGrid>

        <input type="hidden" {...form.register('moeda')} />

        {apiError && <p className="text-sm text-destructive">{apiError}</p>}

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => { clearDraft(); onCancel() }}
          >
            Cancelar
          </Button>
        </div>
      </form>
    </Form>
  )
}
