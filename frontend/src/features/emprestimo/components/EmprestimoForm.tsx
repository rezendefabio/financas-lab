'use client'

import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
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
import { Label } from '@/shared/components/ui/label'
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
import { MoneyInput } from '@/shared/components/MoneyInput'
import { useDraftForm } from '@/shared/hooks/useDraftForm'

// Schema Zod espelhando CriarEmprestimoRequest.java (B6).
// descricao: @NotBlank @Size(max=100); nomeTerceiro: @Size(max=100) opcional;
// tipo: @NotNull enum; valor: @NotNull @Positive (z.number().positive, NUNCA coerce);
// moeda: campo hidden 'BRL'; dataEmprestimo: @NotNull; quitado: boolean.
const schema = z.object({
  descricao: z.string().min(1, 'Descricao e obrigatoria').max(100),
  nomeTerceiro: z.string().max(100).optional(),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.number().positive('Valor deve ser positivo'),
  moeda: z.string(),
  dataEmprestimo: z.string().min(1, 'Data e obrigatoria'),
  quitado: z.boolean(),
})

export type EmprestimoFormValues = z.infer<typeof schema>

const TIPOS: { value: EmprestimoFormValues['tipo']; label: string }[] = [
  { value: 'CONCEDIDO', label: 'Concedido' },
  { value: 'RECEBIDO', label: 'Recebido' },
]

export function defaultEmprestimoFormValues(): EmprestimoFormValues {
  return {
    descricao: '',
    nomeTerceiro: '',
    tipo: 'CONCEDIDO',
    valor: 0,
    moeda: 'BRL',
    dataEmprestimo: '',
    quitado: false,
  }
}

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
    resolver: zodResolver(schema),
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
            <div className="space-y-2">
              <Label htmlFor="emprestimo-tipo">Tipo</Label>
              <Controller
                control={form.control}
                name="tipo"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="emprestimo-tipo" className="w-full">
                      <SelectValue placeholder="Selecione">
                        {(v: string | null) =>
                          TIPOS.find((t) => t.value === v)?.label ?? 'Selecione'
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
            </div>
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
            <div className="space-y-2">
              <Label htmlFor="emprestimo-quitado">Quitado</Label>
              <Controller
                control={form.control}
                name="quitado"
                render={({ field }) => (
                  <div className="flex h-9 items-center">
                    <input
                      id="emprestimo-quitado"
                      type="checkbox"
                      className="accent-primary h-4 w-4"
                      checked={field.value}
                      onChange={(e) => field.onChange(e.target.checked)}
                    />
                  </div>
                )}
              />
            </div>
          </FormCol>
        </FormGrid>

        <input type="hidden" {...form.register('moeda')} />

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
