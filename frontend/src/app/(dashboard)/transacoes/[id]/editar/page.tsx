'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import {
  TransacaoForm,
  type TransacaoFormValues,
} from '@/features/transacoes/components/TransacaoForm'
import type { Transacao } from '@/features/transacoes/types/transacao'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

/** Converte a transacao vinda da API nos valores iniciais do formulario. */
function toFormValues(t: Transacao): TransacaoFormValues {
  return {
    tipo: t.tipo,
    valor: t.valor,
    moeda: t.moeda,
    data: t.data,
    descricao: t.descricao,
    contaId: t.contaId,
    contaDestinoId: t.contaDestinoId ?? undefined,
    categoriaId: t.categoriaId ?? undefined,
    status: t.status,
    payeeId: t.payeeId ?? undefined,
    tagIds: t.tagIds ?? [],
  }
}

export default function EditarTransacaoPage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: transacao, isLoading, isError } = useQuery({
    queryKey: ['transacao', id],
    queryFn: () => transacoesService.buscarPorId(id),
  })

  const mutation = useMutation({
    mutationFn: (values: TransacaoFormValues) =>
      transacoesService.editar(id, {
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
      await queryClient.invalidateQueries({ queryKey: ['transacao', id] })
      router.push('/transacoes')
    },
    onError: () => {
      setApiError('Erro ao salvar transacao.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Transacao</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            {isLoading && (
              <div className="space-y-4">
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
              </div>
            )}

            {isError && (
              <div className="space-y-4">
                <p className="text-sm text-destructive">Transacao nao encontrada.</p>
                <Button variant="outline" onClick={() => router.push('/transacoes')}>
                  Voltar
                </Button>
              </div>
            )}

            {!isLoading && !isError && transacao && (
              <TransacaoForm
                defaultValues={toFormValues(transacao)}
                onSubmit={(v) => mutation.mutate(v)}
                isSubmitting={mutation.isPending}
                apiError={apiError}
                onClearApiError={() => setApiError(null)}
                submitLabel="Salvar"
                onCancel={() => router.push('/transacoes')}
              />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
