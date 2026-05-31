'use client'

import * as React from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  EmprestimoForm,
  emprestimoService,
  type EmprestimoFormValues,
} from '@/features/emprestimo'

export default function EditarEmprestimoPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()
  const [apiError, setApiError] = React.useState<string | null>(null)

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
    onError: () => setApiError('Erro ao salvar o emprestimo.'),
  })

  if (isLoading) {
    return <p className="text-muted-foreground">Carregando...</p>
  }

  if (isError || !data) {
    return <p className="text-destructive">Emprestimo nao encontrado.</p>
  }

  const initialValues: EmprestimoFormValues = {
    descricao: data.descricao,
    nomeTerceiro: data.nomeTerceiro ?? '',
    tipo: data.tipo,
    valor: data.valor.valor,
    moeda: data.valor.moeda,
    dataEmprestimo: data.dataEmprestimo,
    quitado: data.quitado,
  }

  return (
    <div className="space-y-4">
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
