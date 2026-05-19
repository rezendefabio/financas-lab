'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import {
  TransacaoForm,
  defaultTransacaoFormValues,
  type TransacaoFormValues,
} from '@/features/transacoes/components/TransacaoForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

export default function NovaTransacaoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: TransacaoFormValues) =>
      transacoesService.criar({
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        data: values.data,
        descricao: values.descricao,
        contaId: values.contaId,
        ...(values.contaDestinoId ? { contaDestinoId: values.contaDestinoId } : {}),
        ...(values.categoriaId ? { categoriaId: values.categoriaId } : {}),
        status: values.status,
        ...(values.payeeId ? { payeeId: values.payeeId } : {}),
        ...(values.tagIds.length > 0 ? { tagIds: values.tagIds } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transacoes'] })
      router.push('/transacoes')
    },
    onError: () => {
      setApiError('Erro ao registrar transacao.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Transacao</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <TransacaoForm
              defaultValues={defaultTransacaoFormValues()}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar"
              onCancel={() => router.push('/transacoes')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
