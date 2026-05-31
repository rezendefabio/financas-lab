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

// Schema Zod espelha CriarEmprestimoRequest.java / AtualizarEmprestimoRequest.java (B6):
// descricao @NotBlank @Size(max=100) -> .min(1).max(100)
// nomeTerceiro @Size(max=100) (opcional) -> .max(100).optional()
// tipo @NotNull enum -> z.enum
// valor @NotNull positivo (dominio) -> z.coerce.number().positive()
// dataEmprestimo @NotNull LocalDate -> .min(1)
// moeda @NotNull @Size(min=3,max=3) -> hidden, fixo 'BRL'
// quitado boolean -> z.boolean()
export const EmprestimoFormSchema = z.object({
  descricao: z.string().min(1, 'Obrigatorio').max(100, 'Maximo 100 caracteres'),
  nomeTerceiro: z.string().max(100, 'Maximo 100 caracteres').optional(),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().min(3).max(3),
  dataEmprestimo: z.string().min(1, 'Obrigatorio'),
  quitado: z.boolean(),
})

export type EmprestimoFormValues = z.infer<typeof EmprestimoFormSchema>

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
    // Cast obrigatorio -- z.coerce.number() produz schema com input `unknown`.
    resolver: zodResolver(EmprestimoFormSchema) as Resolver<EmprestimoFormValues>,
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
                          TIPOS_EMPRESTIMO.find((t) => t.value === v)?.label ?? 'Selecione'
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
            </FormItem>
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

          <FormCol span={7}>
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

          <FormCol span={5}>
            <FormField
              control={form.control}
              name="quitado"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Quitado</FormLabel>
                  <FormControl>
                    <div className="flex h-9 items-center">
                      <input
                        type="checkbox"
                        className="accent-primary h-4 w-4"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                        id={field.name}
                      />
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
