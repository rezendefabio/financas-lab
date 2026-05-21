'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { contasService } from '@/features/contas/services/contas.service'
import { useDraftForm } from '@/shared/hooks/useDraftForm'
import { Card, CardContent } from '@/shared/components/ui/card'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const schema = z.object({
  nome: z.string().min(1, 'Nome obrigatorio').max(100),
  tipo: z.enum(['CORRENTE', 'POUPANCA', 'DINHEIRO', 'CARTAO_CREDITO', 'INVESTIMENTO', 'OUTRO']),
  saldoInicialValor: z.coerce.number().min(0, 'Valor deve ser >= 0'),
  saldoInicialMoeda: z.string().length(3).default('BRL'),
  limiteCreditoValor: z.coerce.number().min(0).optional(),
  limiteCreditoMoeda: z.string().length(3).default('BRL').optional(),
  diaFechamento: z.coerce.number().min(1).max(31).optional(),
  diaVencimento: z.coerce.number().min(1).max(31).optional(),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'CORRENTE', label: 'Conta Corrente' },
  { value: 'POUPANCA', label: 'Poupanca' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartao de Credito' },
  { value: 'INVESTIMENTO', label: 'Investimento' },
  { value: 'OUTRO', label: 'Outro' },
] as const

export default function NovaConta() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      nome: '',
      tipo: 'CORRENTE',
      saldoInicialValor: 0,
      saldoInicialMoeda: 'BRL',
    },
  })
  const { clearDraft } = useDraftForm(form)

  // eslint-disable-next-line react-hooks/incompatible-library
  const tipoAtual = form.watch('tipo')
  const isCartaoCredito = tipoAtual === 'CARTAO_CREDITO'

  const mutation = useMutation({
    mutationFn: contasService.criar,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['contas'] })
      clearDraft()
      router.push('/contas')
    },
    onError: () => {
      setApiError('Erro ao criar conta.')
    },
  })

  async function onSubmit(values: FormValues) {
    setApiError(null)
    const payload: Parameters<typeof contasService.criar>[0] = {
      nome: values.nome,
      tipo: values.tipo,
      saldoInicialValor: values.saldoInicialValor,
      saldoInicialMoeda: values.saldoInicialMoeda ?? 'BRL',
    }
    if (isCartaoCredito) {
      if (values.limiteCreditoValor !== undefined) {
        payload.limiteCreditoValor = values.limiteCreditoValor
        payload.limiteCreditoMoeda = 'BRL'
      }
      if (values.diaFechamento !== undefined) {
        payload.diaFechamento = values.diaFechamento
      }
      if (values.diaVencimento !== undefined) {
        payload.diaVencimento = values.diaVencimento
      }
    }
    mutation.mutate(payload)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Conta</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormGrid>
                  <FormCol span={8}>
                    <FormField
                      control={form.control}
                      name="nome"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Nome</FormLabel>
                          <FormControl>
                            <Input className="w-full" placeholder="Ex: Conta corrente" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormCol>

                  <FormCol span={4}>
                    <FormItem>
                      <FormLabel>Tipo</FormLabel>
                      <Controller
                        control={form.control}
                        name="tipo"
                        render={({ field }) => (
                          <Select value={field.value} onValueChange={field.onChange}>
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Selecione o tipo">
                                {(v: string | null) => TIPOS.find(t => t.value === v)?.label ?? 'Selecione o tipo'}
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
                      {form.formState.errors.tipo && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.tipo.message}
                        </p>
                      )}
                    </FormItem>
                  </FormCol>

                  <FormCol span={8}>
                    <FormField
                      control={form.control}
                      name="saldoInicialValor"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Saldo inicial (R$)</FormLabel>
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

                  <FormCol span={4}>
                    <FormItem>
                      <FormLabel>Moeda</FormLabel>
                      <FormControl>
                        <Input
                          className="w-full"
                          value={form.watch('saldoInicialMoeda') ?? 'BRL'}
                          readOnly
                          aria-readonly
                        />
                      </FormControl>
                    </FormItem>
                    <input type="hidden" {...form.register('saldoInicialMoeda')} />
                  </FormCol>

                  {isCartaoCredito && (
                    <>
                      <FormCol span={8}>
                        <FormField
                          control={form.control}
                          name="limiteCreditoValor"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Limite de credito (R$)</FormLabel>
                              <FormControl>
                                <MoneyInput
                                  value={field.value ?? 0}
                                  onChange={field.onChange}
                                  id={field.name}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </FormCol>

                      <FormCol span={4}>
                        <FormItem>
                          <FormLabel>Moeda</FormLabel>
                          <FormControl>
                            <Input
                              className="w-full"
                              value="BRL"
                              readOnly
                              aria-readonly
                            />
                          </FormControl>
                        </FormItem>
                      </FormCol>

                      <FormCol span={6}>
                        <FormField
                          control={form.control}
                          name="diaFechamento"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Dia de fechamento</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  min={1}
                                  max={31}
                                  className="w-full"
                                  placeholder="Ex: 25"
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
                          name="diaVencimento"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Dia de vencimento</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  min={1}
                                  max={31}
                                  className="w-full"
                                  placeholder="Ex: 5"
                                  {...field}
                                  value={field.value ?? ''}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </FormCol>
                    </>
                  )}
                </FormGrid>

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button
                    type="submit"
                    disabled={mutation.isPending}
                  >
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => { clearDraft(); router.push('/contas') }}
                  >
                    Cancelar
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
