import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
  emprestimoSchema,
} from './EmprestimoForm'

vi.mock('next/navigation', () => ({
  usePathname: () => '/emprestimos/novo',
}))

describe('EmprestimoForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza campos principais', () => {
    render(
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    expect(screen.getByLabelText(/descricao/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/nome do terceiro/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/data do emprestimo/i)).toBeInTheDocument()
    expect(screen.getByRole('checkbox')).toBeInTheDocument()
    expect(screen.getByText(/quitado/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeInTheDocument()
  })

  it('chama onCancel quando o botao Cancelar e clicado', () => {
    const onCancel = vi.fn()
    render(
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={onCancel}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(onCancel).toHaveBeenCalled()
  })

  it('exibe mensagem de erro quando apiError e fornecido', () => {
    render(
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError="Erro do servidor"
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    expect(screen.getByText(/erro do servidor/i)).toBeInTheDocument()
  })

  it('schema rejeita descricao vazia', () => {
    const res = emprestimoSchema.safeParse({
      descricao: '',
      nomeTerceiro: '',
      tipo: 'CONCEDIDO',
      valor: 10,
      moeda: 'BRL',
      dataEmprestimo: '2026-01-15',
      quitado: false,
    })
    expect(res.success).toBe(false)
  })

  it('schema rejeita valor zero', () => {
    const res = emprestimoSchema.safeParse({
      descricao: 'X',
      nomeTerceiro: '',
      tipo: 'CONCEDIDO',
      valor: 0,
      moeda: 'BRL',
      dataEmprestimo: '2026-01-15',
      quitado: false,
    })
    expect(res.success).toBe(false)
  })

  it('schema aceita dados validos', () => {
    const res = emprestimoSchema.safeParse({
      descricao: 'X',
      nomeTerceiro: 'Joao',
      tipo: 'CONCEDIDO',
      valor: 100,
      moeda: 'BRL',
      dataEmprestimo: '2026-01-15',
      quitado: false,
    })
    expect(res.success).toBe(true)
  })

  it('chama onSubmit com valores normalizados quando formulario valido', async () => {
    const onSubmit = vi.fn()
    render(
      <EmprestimoForm
        defaultValues={{
          descricao: 'Teste',
          nomeTerceiro: 'Joao',
          tipo: 'CONCEDIDO',
          valor: 150,
          moeda: 'BRL',
          dataEmprestimo: '2026-01-15',
          quitado: false,
        }}
        onSubmit={onSubmit}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /salvar/i }))
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled()
    })
  })
})
