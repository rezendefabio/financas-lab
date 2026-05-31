'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { assinaturaService } from '@/features/assinaturas/services/assinatura-service'
import { useAssinatura } from '@/features/assinaturas/hooks/use-assinatura'
import {
  AssinaturaForm,
  type AssinaturaFormValues,
} from '@/features/assinaturas/components/AssinaturaForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

export default function EditarAssinaturaPage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data, isLoading, isError } = useAssinatura(id)

  const mutation = useMutation({
    mutationFn: (v: AssinaturaFormValues) =>
      assinaturaService.atualizar(id, {
        nome: v.nome,
        tipo: v.tipo,
        valorMensal: v.valorMensal,
        moeda: v.moeda,
        dataRenovacao: v.dataRenovacao,
        ativa: v.ativa,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['assinaturas'] })
      await queryClient.invalidateQueries({ queryKey: ['assinatura', id] })
      router.push('/assinaturas')
    },
    onError: () => setApiError('Erro ao atualizar assinatura.'),
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !data) {
    return <p className="text-sm text-destructive">Erro ao carregar assinatura.</p>
  }

  const initialValues: AssinaturaFormValues = {
    nome: data.nome,
    tipo: data.tipo,
    valorMensal: data.valorMensal.valor,
    moeda: data.valorMensal.moeda,
    dataRenovacao: data.dataRenovacao,
    ativa: data.ativa,
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/assinaturas')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Assinatura</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <AssinaturaForm
              defaultValues={initialValues}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar alteracoes"
              onCancel={() => router.push('/assinaturas')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
