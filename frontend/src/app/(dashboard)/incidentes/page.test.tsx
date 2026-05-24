import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/incidente', () => ({
  incidenteService: {
    listar: vi.fn(),
    buscarPorCodigo: vi.fn(),
  },
}))

const mockPush = vi.fn()
let currentParams = new URLSearchParams()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => '/incidentes',
  useSearchParams: () => currentParams,
}))

import IncidentesPage from './page'
import { incidenteService } from '@/features/incidente'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

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
    currentParams = new URLSearchParams()
  })

  it('C1: nao dispara busca automaticamente no mount (URL sem submitted)', () => {
    render(<IncidentesPage />, { wrapper: makeWrapper() })
    expect(incidenteService.listar).not.toHaveBeenCalled()
    expect(incidenteService.buscarPorCodigo).not.toHaveBeenCalled()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('C2: clicar Buscar sem filtros faz push da URL com submitted=1', async () => {
    const user = userEvent.setup()
    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(mockPush).toHaveBeenCalledWith('/incidentes?submitted=1')
  })

  it('C3: filtrar por classeErro inclui param na URL', async () => {
    const user = userEvent.setup()
    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await user.type(
      screen.getByLabelText('Classe do erro'),
      'NullPointerException',
    )
    await user.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(mockPush).toHaveBeenCalledWith(
      '/incidentes?classeErro=NullPointerException&submitted=1',
    )
  })

  it('C4: URL com submitted=1 dispara listar e exibe resultados', async () => {
    currentParams = new URLSearchParams('submitted=1')
    vi.mocked(incidenteService.listar).mockResolvedValue([
      incidente1,
      incidente2,
    ])

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('ERR-ABCD1234')).toBeInTheDocument()
    })
    expect(screen.getByText('ERR-99998888')).toBeInTheDocument()
    expect(incidenteService.listar).toHaveBeenCalledWith({})
  })

  it('C5: URL com classeErro dispara listar com o filtro', async () => {
    currentParams = new URLSearchParams(
      'classeErro=NullPointerException&submitted=1',
    )
    vi.mocked(incidenteService.listar).mockResolvedValue([incidente1])

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(incidenteService.listar).toHaveBeenCalledWith({
        classeErro: 'NullPointerException',
      })
    })
  })

  it('C6: clicar numa linha da tabela expande o stack trace', async () => {
    currentParams = new URLSearchParams('submitted=1')
    vi.mocked(incidenteService.listar).mockResolvedValue([incidente1])
    const user = userEvent.setup()

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    const linha = await screen.findByText('ERR-ABCD1234')
    expect(
      screen.queryByText(/at com\.financas\.App\.run/),
    ).not.toBeInTheDocument()

    await user.click(linha)

    expect(
      screen.getByText(/at com\.financas\.App\.run/),
    ).toBeInTheDocument()
  })

  it('C7: URL com codigo dispara buscarPorCodigo e exibe o detalhe', async () => {
    currentParams = new URLSearchParams('codigo=ERR-ABCD1234&submitted=1')
    vi.mocked(incidenteService.buscarPorCodigo).mockResolvedValue(incidente1)

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('ERR-ABCD1234')).toBeInTheDocument()
    })
    expect(incidenteService.buscarPorCodigo).toHaveBeenCalledWith(
      'ERR-ABCD1234',
    )
  })

  it('C8: busca rapida faz push da URL com codigo e submitted=1', async () => {
    const user = userEvent.setup()
    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await user.type(screen.getByLabelText('Codigo'), 'ERR-ABCD1234')
    await user.click(screen.getByRole('button', { name: 'Ir para incidente' }))

    expect(mockPush).toHaveBeenCalledWith(
      '/incidentes?codigo=ERR-ABCD1234&submitted=1',
    )
  })

  it('C9: resultado vazio exibe mensagem de nenhum incidente encontrado', async () => {
    currentParams = new URLSearchParams('submitted=1')
    vi.mocked(incidenteService.listar).mockResolvedValue([])

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(
        screen.getByText(
          'Nenhum incidente encontrado para os filtros informados.',
        ),
      ).toBeInTheDocument()
    })
  })

  it('C10: limpar filtros faz push para /incidentes sem params', async () => {
    currentParams = new URLSearchParams('classeErro=Foo&submitted=1')
    vi.mocked(incidenteService.listar).mockResolvedValue([])
    const user = userEvent.setup()

    render(<IncidentesPage />, { wrapper: makeWrapper() })

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))

    expect(mockPush).toHaveBeenCalledWith('/incidentes')
  })
})
