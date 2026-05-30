'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
  type EmprestimoFormValues,
} from '@/features/emprestimo/components/EmprestimoForm'
import type { CriarEmprestimoPayload } from '@/features/emprestimo/types/emprestimo'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

function toPayload(values: EmprestimoFormValues): CriarEmprestimoPayload {
  return {
    descricao: values.descricao,
    nomeTerceiro:
      values.nomeTerceiro && values.nomeTerceiro.trim() !== ''
        ? values.nomeTerceiro
        : null,
    tipo: values.tipo,
    valor: values.valor,
    dataEmprestimo: values.dataEmprestimo,
    quitado: values.quitado,
  }
}

export default function NovoEmprestimoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: EmprestimoFormValues) =>
      emprestimoService.criar(toPayload(values)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      router.push('/emprestimos')
    },
    onError: () => setApiError('Erro ao criar emprestimo.'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Voltar"
          onClick={() => router.back()}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">
          Novo emprestimo
        </h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <EmprestimoForm
              defaultValues={defaultEmprestimoFormValues()}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar"
              onCancel={() => router.push('/emprestimos')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
