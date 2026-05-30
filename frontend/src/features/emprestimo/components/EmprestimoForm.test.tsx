import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
} from './EmprestimoForm'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EmprestimoForm', () => {
  it('renderiza botoes Salvar e Cancelar', () => {
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
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  it('renderiza labels dos campos principais', () => {
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
    expect(screen.getByText(/Descricao/i)).toBeInTheDocument()
    expect(screen.getByText(/Tipo/i)).toBeInTheDocument()
    expect(screen.getByText(/Valor/i)).toBeInTheDocument()
    expect(screen.getByText(/Data do emprestimo/i)).toBeInTheDocument()
    expect(screen.getByText(/Quitado/i)).toBeInTheDocument()
  })

  it('exibe mensagem de apiError quando fornecida', () => {
    render(
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
        onSubmit={vi.fn()}
        isSubmitting={false}
        apiError="Erro de teste"
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    expect(screen.getByText('Erro de teste')).toBeInTheDocument()
  })
})
