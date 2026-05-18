import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/incidente', () => ({
  incidenteService: {
    buscarPorCodigo: vi.fn(),
  },
}))

import BuscarIncidentePage from './page'
import { incidenteService } from '@/features/incidente'
import { ApiError } from '@/shared/types/api'

const incidenteMock = {
  id: '11111111-1111-1111-1111-111111111111',
  codigo: 'ERR-ABCD1234',
  operacao: 'POST /api/transacoes',
  classeErro: 'java.lang.IllegalStateException',
  mensagem: 'Saldo insuficiente para a operacao.',
  stackTrace: 'java.lang.IllegalStateException\n\tat com.financas.App.run(App.java:42)',
  criadoEm: '2026-05-18T14:30:00Z',
}

describe('BuscarIncidentePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('C1: mantem o botao Buscar desabilitado enquanto o codigo esta vazio', () => {
    render(<BuscarIncidentePage />)
    expect(screen.getByRole('button', { name: 'Buscar' })).toBeDisabled()
  })

  it('C2: exibe os dados do incidente apos uma busca bem-sucedida', async () => {
    vi.mocked(incidenteService.buscarPorCodigo).mockResolvedValue(incidenteMock)
    const user = userEvent.setup()
    render(<BuscarIncidentePage />)

    await user.type(screen.getByLabelText('Codigo'), 'ERR-ABCD1234')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(screen.getByText('POST /api/transacoes')).toBeInTheDocument()
    })
    expect(
      screen.getByText('java.lang.IllegalStateException'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Saldo insuficiente para a operacao.'),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/at com\.financas\.App\.run/),
    ).toBeInTheDocument()
    expect(incidenteService.buscarPorCodigo).toHaveBeenCalledWith('ERR-ABCD1234')
  })

  it('C3: exibe mensagem de nao encontrado quando o codigo nao existe', async () => {
    vi.mocked(incidenteService.buscarPorCodigo).mockRejectedValue(
      new ApiError(404, 'Nao encontrado'),
    )
    const user = userEvent.setup()
    render(<BuscarIncidentePage />)

    await user.type(screen.getByLabelText('Codigo'), 'ERR-00000000')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(screen.getByText('Codigo nao encontrado.')).toBeInTheDocument()
    })
  })

  it('C4: desabilita o botao e exibe texto de carregamento durante a busca', async () => {
    let resolver: (value: typeof incidenteMock) => void = () => {}
    vi.mocked(incidenteService.buscarPorCodigo).mockReturnValue(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )
    const user = userEvent.setup()
    render(<BuscarIncidentePage />)

    await user.type(screen.getByLabelText('Codigo'), 'ERR-ABCD1234')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    const botaoCarregando = await screen.findByRole('button', {
      name: 'Buscando...',
    })
    expect(botaoCarregando).toBeDisabled()

    resolver(incidenteMock)
    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: 'Buscar' }),
      ).toBeInTheDocument()
    })
  })
})
