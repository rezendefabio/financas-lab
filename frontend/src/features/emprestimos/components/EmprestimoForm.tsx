'use client'
import { Controller, useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { MoneyInput } from '@/shared/components/MoneyInput'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

export const TIPOS_EMPRESTIMO = [
  { value: 'CONCEDIDO', label: 'Concedido' },
  { value: 'RECEBIDO', label: 'Recebido' },
] as const

// Schema Zod inferido de CriarEmprestimoRequest.java (espelhamento Java <-> Zod, B6):
// descricao @NotBlank @Size(max=100); nomeTerceiro @Size(max=100) opcional;
// tipo @NotNull enum; valor @NotNull BigDecimal positivo; moeda fixo BRL;
// dataEmprestimo @NotNull LocalDate; quitado boolean (atualizacao).
export const emprestimoFormSchema = z.object({
  descricao: z.string().min(1, 'Obrigatorio').max(100, 'Maximo 100 caracteres'),
  nomeTerceiro: z.string().max(100, 'Maximo 100 caracteres').optional(),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3),
  dataEmprestimo: z.string().min(1, 'Obrigatorio'),
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
    resolver: zodResolver(emprestimoFormSchema) as Resolver<EmprestimoFormValues>,
    defaultValues,
  })
  const { clearDraft } = useDraftForm(form)

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
                    <Input maxLength={100} {...field} value={field.value ?? ''} />
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
              render={() => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Controller
                    control={form.control}
                    name="tipo"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="Selecione">
                            {(v: string | null) =>
                              TIPOS_EMPRESTIMO.find((t) => t.value === v)?.label ??
                              'Selecione'
                            }
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {TIPOS_EMPRESTIMO.map((t) => (
                            <SelectItem key={t.value} value={t.value}>
                              {t.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
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
                  <FormLabel htmlFor="quitado">Quitado</FormLabel>
                  <FormControl>
                    <div className="flex items-center gap-2 pt-1">
                      <input
                        id="quitado"
                        type="checkbox"
                        className="h-4 w-4 rounded border-input"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                      <span className="text-sm text-muted-foreground">
                        {field.value ? 'Sim' : 'Nao'}
                      </span>
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
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
