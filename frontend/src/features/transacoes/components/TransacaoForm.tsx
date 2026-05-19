'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { contasService } from '@/features/contas/services/contas.service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { listarPayees } from '@/features/payee/services/payee-service'
import { listarTags } from '@/features/tag/services/tag-service'
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
import { MoneyInput } from '@/shared/components/MoneyInput'
import { FormGrid } from '@/shared/components/FormGrid'
import { FormCol } from '@/shared/components/FormCol'
import { LookupField } from '@/shared/components/LookupField'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const STATUS_OPTIONS = [
  { value: 'CLEARED', label: 'Confirmada' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'SCHEDULED', label: 'Agendada' },
  { value: 'CANCELLED', label: 'Cancelada' },
] as const

/**
 * Schema espelhando `TransacaoRequest.java` (reusado por POST e PUT no backend):
 * - `valor` `@NotNull` + dominio exige positivo -> `.positive()`
 * - `moeda` `@NotNull @Size(min=3,max=3)` -> `.length(3)`
 * - `data` `@NotNull` -> `.min(1)`
 * - `descricao` `@NotBlank @Size(max=200)` -> `.min(1).max(200)`
 * - `contaId` `@NotNull` -> `.uuid()`
 */
const schema = z.object({
  tipo: z.enum(['RECEITA', 'DESPESA', 'TRANSFERENCIA']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3).default('BRL'),
  data: z.string().min(1, 'Data obrigatoria'),
  descricao: z.string().min(1, 'Descricao obrigatoria').max(200),
  contaId: z.string().uuid('Selecione uma conta'),
  contaDestinoId: z.string().uuid().optional(),
  categoriaId: z.string().uuid().optional(),
  status: z.enum(['CLEARED', 'PENDING', 'SCHEDULED', 'CANCELLED']).default('CLEARED'),
  payeeId: z.string().uuid().optional(),
  tagIds: z.array(z.string().uuid()).default([]),
})

export type TransacaoFormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
] as const

/** Valores iniciais padrao de uma transacao nova (data = hoje). */
export function defaultTransacaoFormValues(): TransacaoFormValues {
  return {
    tipo: 'DESPESA',
    valor: 0,
    moeda: 'BRL',
    data: new Date().toISOString().slice(0, 10),
    descricao: '',
    contaId: '',
    contaDestinoId: undefined,
    categoriaId: undefined,
    status: 'CLEARED',
    payeeId: undefined,
    tagIds: [],
  }
}

interface TransacaoFormProps {
  /** Valores iniciais do formulario (criacao usa defaults, edicao usa dados existentes). */
  defaultValues: TransacaoFormValues
  /** Chamado com os valores validados ao submeter. */
  onSubmit: (values: TransacaoFormValues) => void
  /** Indica que a submissao esta em andamento (desabilita o botao Salvar). */
  isSubmitting: boolean
  /** Mensagem de erro vinda da API, exibida acima dos botoes. */
  apiError: string | null
  /** Limpa o erro de API ao iniciar nova submissao. */
  onClearApiError: () => void
  /** Texto do botao de submit. */
  submitLabel: string
  /** Callback do botao Cancelar. */
  onCancel: () => void
}

/**
 * Formulario compartilhado de transacao, usado pelas paginas de criacao e edicao.
 * A estrutura de campos e identica em ambos os fluxos.
 *
 * Layout em 12 colunas (FormGrid + FormCol):
 *  - tipo (12)
 *  - valor (7) + data (5)
 *  - descricao (12)
 *  - contaId (6) + contaDestinoId (6, condicional TRANSFERENCIA)
 *  - categoriaId (6) + status (6)
 *  - payeeId (12)
 *  - tagIds (12, checkboxes inline)
 *
 * FKs (contaId, contaDestinoId, categoriaId, payeeId) usam `LookupField` que
 * gerencia o proprio `useQuery` internamente -- nao ha mais `useQuery`
 * separado para popular Selects. `categoriaId` usa `queryKey` com `tipoAtual`
 * para refetch ao mudar o tipo.
 *
 * `tagIds` mantem checkboxes (multi-select) -- LookupField e single-select.
 */
export function TransacaoForm({
  defaultValues,
  onSubmit,
  isSubmitting,
  apiError,
  onClearApiError,
  submitLabel,
  onCancel,
}: TransacaoFormProps) {
  const form = useForm<TransacaoFormValues>({
    resolver: zodResolver(schema) as Resolver<TransacaoFormValues>,
    defaultValues,
  })

  // eslint-disable-next-line react-hooks/incompatible-library
  const tipoAtual = form.watch('tipo')
  const tagIdsAtual = form.watch('tagIds')
  const isTransferencia = tipoAtual === 'TRANSFERENCIA'

  // Tags continuam usando useQuery direto -- multi-select via checkboxes
  // nao se encaixa no LookupField (single-select).
  const { data: tags, isLoading: tagsLoading } = useQuery({
    queryKey: ['tags'],
    queryFn: listarTags,
  })

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((v) => { onClearApiError(); onSubmit(v) })}
        className="space-y-4"
      >
        <FormGrid>
          {/* Tipo */}
          <FormCol span={12}>
            <FormItem>
              <FormLabel>Tipo</FormLabel>
              <Controller
                control={form.control}
                name="tipo"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue>
                        {(v: string | null) => TIPOS.find(t => t.value === v)?.label ?? 'Selecione o tipo'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {TIPOS.map((t) => (
                        <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </FormItem>
          </FormCol>

          {/* Valor */}
          <FormCol span={7}>
            <FormField
              control={form.control}
              name="valor"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Valor (R$)</FormLabel>
                  <FormControl>
                    <MoneyInput value={field.value} onChange={field.onChange} id={field.name} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          {/* Data */}
          <FormCol span={5}>
            <FormField
              control={form.control}
              name="data"
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

          {/* Descricao */}
          <FormCol span={12}>
            <FormField
              control={form.control}
              name="descricao"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descricao</FormLabel>
                  <FormControl>
                    <Input className="w-full" placeholder="Ex: Supermercado" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </FormCol>

          {/* Conta origem */}
          <FormCol span={isTransferencia ? 6 : 12}>
            <FormItem>
              <FormLabel>Conta {isTransferencia ? 'de origem' : ''}</FormLabel>
              <Controller
                control={form.control}
                name="contaId"
                render={({ field }) => (
                  <LookupField
                    value={field.value || null}
                    onChange={(v) => field.onChange(v ?? '')}
                    queryKey={['contas', 'ativas']}
                    queryFn={() =>
                      contasService
                        .listar()
                        .then((cs) =>
                          cs
                            .filter((c) => c.ativa)
                            .map((c) => ({ value: c.id, label: c.nome })),
                        )
                    }
                    placeholder="Selecione a conta"
                  />
                )}
              />
              {form.formState.errors.contaId && (
                <p className="text-sm text-destructive">{form.formState.errors.contaId.message}</p>
              )}
            </FormItem>
          </FormCol>

          {/* Conta destino (so para transferencia) */}
          {isTransferencia && (
            <FormCol span={6}>
              <FormItem>
                <FormLabel>Conta de destino</FormLabel>
                <Controller
                  control={form.control}
                  name="contaDestinoId"
                  render={({ field }) => (
                    <LookupField
                      value={field.value ?? null}
                      onChange={(v) => field.onChange(v ?? undefined)}
                      queryKey={['contas', 'ativas']}
                      queryFn={() =>
                        contasService
                          .listar()
                          .then((cs) =>
                            cs
                              .filter((c) => c.ativa)
                              .map((c) => ({ value: c.id, label: c.nome })),
                          )
                      }
                      placeholder="Selecione a conta de destino"
                    />
                  )}
                />
              </FormItem>
            </FormCol>
          )}

          {/* Categoria (opcional, oculto para transferencia) */}
          {!isTransferencia && (
            <FormCol span={6}>
              <FormItem>
                <FormLabel>Categoria (opcional)</FormLabel>
                <Controller
                  control={form.control}
                  name="categoriaId"
                  render={({ field }) => (
                    <LookupField
                      value={field.value ?? null}
                      onChange={(v) => field.onChange(v ?? undefined)}
                      queryKey={['categorias', tipoAtual]}
                      queryFn={() =>
                        categoriasService.listar().then((cats) => {
                          const doTipo = cats.filter((c) => c.tipo === tipoAtual)
                          const unicas = doTipo.filter(
                            (c, idx, arr) =>
                              arr.findIndex((x) => x.nome === c.nome) === idx,
                          )
                          return unicas.map((c) => ({ value: c.id, label: c.nome }))
                        })
                      }
                      placeholder="Sem categoria"
                      emptyMessage="Nenhuma categoria para este tipo."
                    />
                  )}
                />
              </FormItem>
            </FormCol>
          )}

          {/* Status */}
          <FormCol span={isTransferencia ? 12 : 6}>
            <FormItem>
              <FormLabel>Status</FormLabel>
              <Controller
                control={form.control}
                name="status"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue>
                        {(v: string | null) => STATUS_OPTIONS.find(s => s.value === v)?.label ?? 'Selecione o status'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {STATUS_OPTIONS.map((s) => (
                        <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </FormItem>
          </FormCol>

          {/* Payee (opcional) */}
          <FormCol span={12}>
            <FormItem>
              <FormLabel>Beneficiario (opcional)</FormLabel>
              <Controller
                control={form.control}
                name="payeeId"
                render={({ field }) => (
                  <LookupField
                    value={field.value ?? null}
                    onChange={(v) => field.onChange(v ?? undefined)}
                    queryKey={['payees']}
                    queryFn={() =>
                      listarPayees().then((ps) =>
                        ps.map((p) => ({ value: p.id, label: p.nome })),
                      )
                    }
                    placeholder="Sem beneficiario"
                    emptyMessage="Nenhum beneficiario cadastrado."
                  />
                )}
              />
            </FormItem>
          </FormCol>

          {/* Tags (multi-select via checkboxes) */}
          {!tagsLoading && (tags ?? []).length > 0 && (
            <FormCol span={12}>
              <FormItem>
                <FormLabel>Tags</FormLabel>
                <div className="flex flex-wrap gap-2 pt-1">
                  {(tags ?? []).map((tag) => {
                    const checked = tagIdsAtual.includes(tag.id)
                    return (
                      <label
                        key={tag.id}
                        className="flex items-center gap-1.5 cursor-pointer select-none text-sm"
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(e) => {
                            const current = form.getValues('tagIds')
                            if (e.target.checked) {
                              form.setValue('tagIds', [...current, tag.id])
                            } else {
                              form.setValue('tagIds', current.filter(id => id !== tag.id))
                            }
                          }}
                          className="accent-primary"
                        />
                        {tag.cor && (
                          <span
                            className="inline-block w-3 h-3 rounded-full"
                            style={{ backgroundColor: tag.cor }}
                            aria-hidden="true"
                          />
                        )}
                        {tag.nome}
                      </label>
                    )
                  })}
                </div>
              </FormItem>
            </FormCol>
          )}
        </FormGrid>

        <input type="hidden" {...form.register('moeda')} />

        {apiError && (
          <p className="text-sm text-destructive">{apiError}</p>
        )}

        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : submitLabel}
          </Button>
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        </div>
      </form>
    </Form>
  )
}
