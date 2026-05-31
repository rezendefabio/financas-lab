'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  EmprestimoForm,
  emprestimoService,
  type EmprestimoFormValues,
} from '@/features/emprestimo'

export default function EditarEmprestimoPage() {
  const params = useParams<{ id: string }>()
  const id = params.id
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['emprestimos', id],
    queryFn: () => emprestimoService.buscarPorId(id),
  })

  const mutation = useMutation({
    mutationFn: (values: EmprestimoFormValues) =>
      emprestimoService.atualizar(id, {
        descricao: values.descricao,
        nomeTerceiro: values.nomeTerceiro || undefined,
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
    onError: () => setApiError('Erro ao salvar emprestimo.'),
  })

  if (isLoading) {
    return <Skeleton className="h-64 w-full max-w-2xl" />
  }
  if (isError || !data) {
    return <p className="text-destructive">Erro ao carregar emprestimo.</p>
  }

  // Derivar initialValues SINCRONAMENTE apos o early-return (sem useEffect+setState).
  const initialValues: EmprestimoFormValues = {
    descricao: data.descricao,
    nomeTerceiro: data.nomeTerceiro ?? '',
    tipo: data.tipo,
    valor: data.valor.valor,
    dataEmprestimo: data.dataEmprestimo,
    quitado: data.quitado,
    moeda: data.valor.moeda,
  }

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Editar Emprestimo</h1>
      <EmprestimoForm
        defaultValues={initialValues}
        onSubmit={mutation.mutate}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar"
        onCancel={() => router.push('/emprestimos')}
      />
    </div>
  )
}
