import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/incidente', () => ({
  incidenteService: {
    listar: vi.fn(),
    buscarPorCodigo: vi.fn(),
  },
}))

import IncidentesPage from './page'
import { incidenteService } from '@/features/incidente'
import { ApiError } from '@/shared/types/api'

const incidente1 = {
  id: '11111111-1111-1111-1111-111111111111',
  codigo: 'ERR-ABCD1234',
  operacao: 'POST /api/transacoes',
  classeErro: 'java.lang.NullPointerException',
  mensagem: 'Saldo nulo na operacao.',
  stackTrace:
    'java.lang.NullPointerException\n\tat com.financas.App.run(App.java:42)',
  criadoEm: '2026-05-18T14:30:00Z',
}

const incidente2 = {
  id: '22222222-2222-2222-2222-222222222222',
  codigo: 'ERR-99998888',
  operacao: 'GET /api/contas',
  classeErro: 'java.lang.RuntimeException',
  mensagem: 'Falha generica.',
  stackTrace:
    'java.lang.RuntimeException\n\tat com.financas.Svc.go(Svc.java:7)',
  criadoEm: '2026-05-17T09:00:00Z',
}

describe('IncidentesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('C1: nao dispara busca automaticamente no mount', () => {
    render(<IncidentesPage />)
    expect(incidenteService.listar).not.toHaveBeenCalled()
    expect(incidenteService.buscarPorCodigo).not.toHaveBeenCalled()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('C2: clicar Buscar sem filtros chama listar({}) e exibe resultados', async () => {
    vi.mocked(incidenteService.listar).mockResolvedValue([
      incidente1,
      incidente2,
    ])
    const user = userEvent.setup()
    render(<IncidentesPage />)

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(screen.getByText('ERR-ABCD1234')).toBeInTheDocument()
    })
    expect(screen.getByText('ERR-99998888')).toBeInTheDocument()
    expect(incidenteService.listar).toHaveBeenCalledWith({})
  })

  it('C3: filtrar por classeErro envia o filtro ao service', async () => {
    vi.mocked(incidenteService.listar).mockResolvedValue([incidente1])
    const user = userEvent.setup()
    render(<IncidentesPage />)

    await user.type(
      screen.getByLabelText('Classe do erro'),
      'NullPointerException',
    )
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(incidenteService.listar).toHaveBeenCalledWith({
        classeErro: 'NullPointerException',
      })
    })
  })

  it('C4: clicar numa linha da tabela expande o stack trace', async () => {
    vi.mocked(incidenteService.listar).mockResolvedValue([incidente1])
    const user = userEvent.setup()
    render(<IncidentesPage />)

    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    const linha = await screen.findByText('ERR-ABCD1234')

    expect(
      screen.queryByText(/at com\.financas\.App\.run/),
    ).not.toBeInTheDocument()

    await user.click(linha)

    expect(
      screen.getByText(/at com\.financas\.App\.run/),
    ).toBeInTheDocument()
  })

  it('C5: busca rapida por codigo chama buscarPorCodigo e exibe o detalhe', async () => {
    vi.mocked(incidenteService.buscarPorCodigo).mockResolvedValue(incidente1)
    const user = userEvent.setup()
    render(<IncidentesPage />)

    await user.type(screen.getByLabelText('Codigo'), 'ERR-ABCD1234')
    await user.click(screen.getByRole('button', { name: 'Ir para incidente' }))

    await waitFor(() => {
      expect(
        screen.getByText(/at com\.financas\.App\.run/),
      ).toBeInTheDocument()
    })
    expect(incidenteService.buscarPorCodigo).toHaveBeenCalledWith(
      'ERR-ABCD1234',
    )
  })

  it('C6: resultado vazio exibe mensagem de nenhum incidente encontrado', async () => {
    vi.mocked(incidenteService.listar).mockResolvedValue([])
    const user = userEvent.setup()
    render(<IncidentesPage />)

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(
        screen.getByText(
          'Nenhum incidente encontrado para os filtros informados.',
        ),
      ).toBeInTheDocument()
    })
  })
})
