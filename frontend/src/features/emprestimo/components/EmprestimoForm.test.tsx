import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { EmprestimoForm, defaultEmprestimoFormValues } from './EmprestimoForm'

vi.mock('next/navigation', () => ({
  usePathname: () => '/emprestimos/novo',
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('EmprestimoForm', () => {
  it('renderiza campos do dominio e botoes Salvar/Cancelar', () => {
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
    expect(screen.getByText('Descricao')).toBeInTheDocument()
    expect(screen.getByText('Nome do terceiro')).toBeInTheDocument()
    expect(screen.getByText('Tipo')).toBeInTheDocument()
    expect(screen.getByText('Data do emprestimo')).toBeInTheDocument()
    expect(screen.getByText('Valor (R$)')).toBeInTheDocument()
    expect(screen.getByText('Quitado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
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

  it('chama onCancel ao clicar em Cancelar', async () => {
    const onCancel = vi.fn()
    const { default: userEvent } = await import('@testing-library/user-event')
    const user = userEvent.setup()
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
    await user.click(screen.getByRole('button', { name: /Cancelar/i }))
    expect(onCancel).toHaveBeenCalled()
  })
})
