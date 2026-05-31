import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AssinaturaForm, defaultAssinaturaFormValues } from './AssinaturaForm'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('AssinaturaForm', () => {
  it('renderiza botoes Salvar e Cancelar e campos principais', () => {
    render(
      <AssinaturaForm
        defaultValues={defaultAssinaturaFormValues()}
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
    expect(screen.getByLabelText(/Nome do servico/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Data de renovacao/i)).toBeInTheDocument()
  })

  it('exibe mensagem de apiError quando fornecida', () => {
    render(
      <AssinaturaForm
        defaultValues={defaultAssinaturaFormValues()}
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

  it('submit valido chama onSubmit com os valores', async () => {
    const onSubmit = vi.fn()
    render(
      <AssinaturaForm
        defaultValues={{
          nome: 'Netflix',
          tipo: 'STREAMING',
          valorMensal: 29.9,
          moeda: 'BRL',
          dataRenovacao: '2026-06-15',
          ativa: true,
        }}
        onSubmit={onSubmit}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /Salvar/i }))
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledTimes(1)
    })
    expect(onSubmit.mock.calls[0][0]).toMatchObject({ nome: 'Netflix', tipo: 'STREAMING' })
  })

  it('campo nome vazio bloqueia submit', async () => {
    const onSubmit = vi.fn()
    render(
      <AssinaturaForm
        defaultValues={defaultAssinaturaFormValues()}
        onSubmit={onSubmit}
        isSubmitting={false}
        apiError={null}
        onClearApiError={vi.fn()}
        submitLabel="Salvar"
        onCancel={vi.fn()}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /Salvar/i }))
    await waitFor(() => {
      expect(screen.getAllByText('Obrigatorio').length).toBeGreaterThan(0)
    })
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
