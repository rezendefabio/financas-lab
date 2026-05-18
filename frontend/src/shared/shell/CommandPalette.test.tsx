import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, beforeEach, vi } from 'vitest'

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import { CommandPalette } from './CommandPalette'

async function pressCtrlK() {
  await userEvent.keyboard('{Control>}k{/Control}')
}

describe('CommandPalette', () => {
  beforeEach(() => {
    mockPush.mockClear()
  })

  it('inicia fechado', () => {
    render(<CommandPalette />)
    expect(screen.queryByPlaceholderText(/buscar tela/i)).not.toBeInTheDocument()
  })

  it('abre ao pressionar Ctrl+K', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    expect(screen.getByPlaceholderText(/buscar tela/i)).toBeInTheDocument()
  })

  it('lista as telas do registry quando aberto', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    expect(screen.getByText('Contas')).toBeInTheDocument()
    expect(screen.getByText('Transacoes')).toBeInTheDocument()
    expect(screen.getByText('FIN-CTA-001')).toBeInTheDocument()
  })

  it('filtra resultados conforme o texto digitado', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    await userEvent.type(screen.getByPlaceholderText(/buscar tela/i), 'contas')
    expect(screen.getByText('Contas')).toBeInTheDocument()
    expect(screen.queryByText('Incidentes')).not.toBeInTheDocument()
  })

  it('filtra por codigo de tela', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    await userEvent.type(
      screen.getByPlaceholderText(/buscar tela/i),
      'FIN-TRX-001',
    )
    expect(screen.getByText('Transacoes')).toBeInTheDocument()
    expect(screen.queryByText('Contas')).not.toBeInTheDocument()
  })

  it('navega para a rota da tela selecionada e fecha o palette', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    await userEvent.click(screen.getByText('Contas'))
    expect(mockPush).toHaveBeenCalledWith('/contas')
    expect(screen.queryByPlaceholderText(/buscar tela/i)).not.toBeInTheDocument()
  })

  it('exibe mensagem vazia quando nenhum resultado casa', async () => {
    render(<CommandPalette />)
    await pressCtrlK()
    await userEvent.type(
      screen.getByPlaceholderText(/buscar tela/i),
      'zzznaoexiste',
    )
    expect(screen.getByText(/nenhuma tela encontrada/i)).toBeInTheDocument()
  })
})
