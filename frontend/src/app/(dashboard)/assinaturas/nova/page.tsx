'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { assinaturaService } from '@/features/assinaturas/services/assinatura-service'
import {
  AssinaturaForm,
  defaultAssinaturaFormValues,
  type AssinaturaFormValues,
} from '@/features/assinaturas/components/AssinaturaForm'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

export default function NovaAssinaturaPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: AssinaturaFormValues) =>
      assinaturaService.criar({
        nome: values.nome,
        tipo: values.tipo,
        valorMensal: values.valorMensal,
        moeda: values.moeda,
        dataRenovacao: values.dataRenovacao,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['assinaturas'] })
      router.push('/assinaturas')
    },
    onError: () => setApiError('Erro ao criar assinatura.'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Assinatura</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <AssinaturaForm
              defaultValues={defaultAssinaturaFormValues()}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar"
              onCancel={() => router.push('/assinaturas')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
