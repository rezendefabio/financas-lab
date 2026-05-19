import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuditLogDrawer } from './AuditLogDrawer'
import type { AuditLogPage } from '../types/auditlog'

vi.mock('../services/auditlog-service', () => ({
  auditlogService: {
    listarPorEntidade: vi.fn(),
  },
}))

import { auditlogService } from '../services/auditlog-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

function renderDrawer(entityId: string | null = 'conta-1') {
  return render(
    <AuditLogDrawer
      open
      onOpenChange={() => {}}
      entityType="conta"
      entityId={entityId}
    />,
    { wrapper: makeWrapper() },
  )
}

const pagina = (overrides?: Partial<AuditLogPage>): AuditLogPage => ({
  content: [
    {
      id: 'log-1',
      entityType: 'conta',
      entityId: 'conta-1',
      action: 'UPDATE',
      userEmail: 'user@exemplo.com',
      screenCode: 'FIN-CTA-001',
      before: '{"nome":"Antiga"}',
      after: '{"nome":"Nova"}',
      criadoEm: '2026-05-18T10:00:00Z',
    },
  ],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  ...overrides,
})

describe('AuditLogDrawer', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renderiza as entradas da trilha de auditoria', async () => {
    vi.mocked(auditlogService.listarPorEntidade).mockResolvedValue(pagina())

    renderDrawer()

    await waitFor(() =>
      expect(screen.getByText(/user@exemplo\.com - atualizou/)).toBeInTheDocument(),
    )
    expect(screen.getByText('FIN-CTA-001')).toBeInTheDocument()
  })

  it('expande o diff ao clicar em "Ver detalhes"', async () => {
    vi.mocked(auditlogService.listarPorEntidade).mockResolvedValue(pagina())
    const user = userEvent.setup()

    renderDrawer()

    await waitFor(() => expect(screen.getByText('Ver detalhes')).toBeInTheDocument())
    await user.click(screen.getByText('Ver detalhes'))

    expect(screen.getByText('Antes')).toBeInTheDocument()
    expect(screen.getByText('Depois')).toBeInTheDocument()
  })

  it('mostra estado vazio quando nao ha historico', async () => {
    vi.mocked(auditlogService.listarPorEntidade).mockResolvedValue(
      pagina({ content: [], totalElements: 0, totalPages: 0 }),
    )

    renderDrawer()

    await waitFor(() =>
      expect(screen.getByText('Nenhum historico encontrado.')).toBeInTheDocument(),
    )
  })

  it('exibe "Carregar mais" e busca a proxima pagina ao clicar', async () => {
    const entradaPagina2 = {
      ...pagina().content[0],
      id: 'log-2',
      action: 'CREATE' as const,
    }
    vi.mocked(auditlogService.listarPorEntidade)
      .mockResolvedValueOnce(pagina({ totalPages: 2, totalElements: 2 }))
      .mockResolvedValueOnce(
        pagina({ content: [entradaPagina2], number: 1, totalPages: 2, totalElements: 2 }),
      )
    const user = userEvent.setup()

    renderDrawer()

    await waitFor(() => expect(screen.getByText('Carregar mais')).toBeInTheDocument())
    await user.click(screen.getByText('Carregar mais'))

    await waitFor(() =>
      expect(screen.getByText(/user@exemplo\.com - criou/)).toBeInTheDocument(),
    )
    expect(auditlogService.listarPorEntidade).toHaveBeenCalledTimes(2)
  })
})
