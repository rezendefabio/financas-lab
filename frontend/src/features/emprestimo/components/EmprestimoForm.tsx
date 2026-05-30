'use client'
import { useForm, type Resolver } from 'react-hook-form'
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

// Schema Zod espelhando CriarEmprestimoRequest.java (B6).
// @NotBlank @Size(max=100) descricao
// @Size(max=100) nomeTerceiro (opcional)
// @NotNull TipoEmprestimo tipo
// @NotNull @Positive BigDecimal valor
// @NotNull LocalDate dataEmprestimo
// boolean quitado
export const EmprestimoFormSchema = z.object({
  descricao: z
    .string()
    .min(1, 'Descricao e obrigatoria')
    .max(100, 'Maximo 100 caracteres'),
  nomeTerceiro: z
    .string()
    .max(100, 'Maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  dataEmprestimo: z.string().min(1, 'Data e obrigatoria'),
  quitado: z.boolean(),
})

export type EmprestimoFormValues = z.infer<typeof EmprestimoFormSchema>

export function defaultEmprestimoFormValues(): EmprestimoFormValues {
  return {
    descricao: '',
    nomeTerceiro: '',
    tipo: 'CONCEDIDO',
    valor: 0,
    dataEmprestimo: '',
    quitado: false,
  } as EmprestimoFormValues
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
                  <Select
                    onValueChange={field.onChange}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o tipo" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="CONCEDIDO">Concedido</SelectItem>
                      <SelectItem value="RECEBIDO">Recebido</SelectItem>
                    </SelectContent>
                  </Select>
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
                  <FormLabel>Valor</FormLabel>
                  <FormControl>
                    <MoneyInput
                      value={Number(field.value) || 0}
                      onChange={field.onChange}
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

          <FormCol span={12}>
            <FormField
              control={form.control}
              name="quitado"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center gap-2 space-y-0">
                  <FormControl>
                    <input
                      type="checkbox"
                      checked={field.value}
                      onChange={(e) => field.onChange(e.target.checked)}
                      className="h-4 w-4"
                    />
                  </FormControl>
                  <FormLabel className="!mt-0">Quitado</FormLabel>
                  <FormMessage />
                </FormItem>
              )}
            />
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
