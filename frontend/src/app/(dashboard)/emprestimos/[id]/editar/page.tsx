'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { emprestimoService } from '@/features/emprestimo/services/emprestimo-service'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
  type EmprestimoFormValues,
} from '@/features/emprestimo/components/EmprestimoForm'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Skeleton } from '@/shared/components/ui/skeleton'

export default function EditarEmprestimoPage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)
  const [initialValues, setInitialValues] =
    useState<EmprestimoFormValues | null>(null)

  const { data: emprestimo, isLoading, isError } = useQuery({
    queryKey: ['emprestimos', id],
    queryFn: () => emprestimoService.buscar(id),
  })

  useEffect(() => {
    if (emprestimo && !initialValues) {
      setInitialValues({
        descricao: emprestimo.descricao,
        nomeTerceiro: emprestimo.nomeTerceiro ?? '',
        tipo: emprestimo.tipo,
        valor: emprestimo.valor.valor,
        moeda: emprestimo.valor.moeda,
        dataEmprestimo: emprestimo.dataEmprestimo,
        quitado: emprestimo.quitado,
      })
    }
  }, [emprestimo, initialValues])

  const mutation = useMutation({
    mutationFn: (values: EmprestimoFormValues) =>
      emprestimoService.atualizar(id, {
        descricao: values.descricao,
        nomeTerceiro: values.nomeTerceiro ? values.nomeTerceiro : null,
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        dataEmprestimo: values.dataEmprestimo,
        quitado: values.quitado,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      await queryClient.invalidateQueries({ queryKey: ['emprestimos', id] })
      router.push('/emprestimos')
    },
    onError: () => setApiError('Erro ao atualizar emprestimo.'),
  })

  if (isLoading || (!initialValues && !isError)) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full max-w-2xl" />
      </div>
    )
  }

  if (isError) {
    return <p className="text-sm text-destructive">Erro ao carregar emprestimo.</p>
  }

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
        <h1 className="text-2xl font-semibold tracking-tight">Editar emprestimo</h1>
      </div>

      <div className="max-w-2xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <EmprestimoForm
              defaultValues={initialValues ?? defaultEmprestimoFormValues()}
              onSubmit={mutation.mutate}
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
