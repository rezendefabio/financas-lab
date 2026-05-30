'use client'
import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'
import { useEmprestimo } from '@/features/emprestimo/hooks/use-emprestimo'
import {
  EmprestimoForm,
  type EmprestimoFormValues,
} from '@/features/emprestimo/components/EmprestimoForm'
import type {
  AtualizarEmprestimoPayload,
  Emprestimo,
} from '@/features/emprestimo/types/emprestimo'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

function toFormValues(data: Emprestimo): EmprestimoFormValues {
  return {
    descricao: data.descricao,
    nomeTerceiro: data.nomeTerceiro ?? '',
    tipo: data.tipo,
    valor: data.valor.valor,
    dataEmprestimo: data.dataEmprestimo,
    quitado: data.quitado,
  }
}

function toPayload(values: EmprestimoFormValues): AtualizarEmprestimoPayload {
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

export default function EditarEmprestimoPage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data, isLoading, isError } = useEmprestimo(id)

  const mutation = useMutation({
    mutationFn: (v: EmprestimoFormValues) =>
      emprestimoService.atualizar(id, toPayload(v)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      await queryClient.invalidateQueries({ queryKey: ['emprestimo', id] })
      router.push('/emprestimos')
    },
    onError: () => setApiError('Erro ao atualizar emprestimo.'),
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
    return (
      <p className="text-sm text-destructive">Erro ao carregar emprestimo.</p>
    )
  }

  const initialValues: EmprestimoFormValues = toFormValues(data)

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Voltar"
          onClick={() => router.push('/emprestimos')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">
          Editar emprestimo
        </h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <EmprestimoForm
              defaultValues={initialValues}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar alteracoes"
              onCancel={() => router.push('/emprestimos')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
