import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
} from './EmprestimoForm'

beforeEach(() => {
  vi.clearAllMocks()
})

function renderForm(overrides: Partial<React.ComponentProps<typeof EmprestimoForm>> = {}) {
  const props = {
    defaultValues: defaultEmprestimoFormValues(),
    onSubmit: vi.fn(),
    isSubmitting: false,
    apiError: null,
    onClearApiError: vi.fn(),
    submitLabel: 'Salvar',
    onCancel: vi.fn(),
    ...overrides,
  }
  render(<EmprestimoForm {...props} />)
  return props
}

describe('EmprestimoForm', () => {
  it('renderiza botoes Salvar e Cancelar', () => {
    renderForm()
    expect(screen.getByRole('button', { name: /Salvar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument()
  })

  it('exibe mensagem de apiError quando fornecida', () => {
    renderForm({ apiError: 'Erro de teste' })
    expect(screen.getByText('Erro de teste')).toBeInTheDocument()
  })

  it('nao chama onSubmit com valores invalidos (validacao Zod)', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm()
    await user.click(screen.getByRole('button', { name: /Salvar/i }))
    // descricao vazia, valor 0 e data vazia falham a validacao Zod (B6).
    await waitFor(() => {
      expect(onSubmit).not.toHaveBeenCalled()
    })
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('chama onSubmit com valores validos', async () => {
    const user = userEvent.setup()
    const { onSubmit } = renderForm({
      defaultValues: {
        descricao: 'Emprestimo ao Joao',
        nomeTerceiro: 'Joao',
        tipo: 'CONCEDIDO',
        valor: 100,
        moeda: 'BRL',
        dataEmprestimo: '2026-05-30',
        quitado: false,
      },
    })
    await user.click(screen.getByRole('button', { name: /Salvar/i }))
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledTimes(1)
    })
  })
})
