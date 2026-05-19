import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

const auditDrawerMock = vi.fn()
vi.mock('@/features/auditlog', () => ({
  AuditLogDrawer: (props: { open: boolean; entityType: string }) => {
    auditDrawerMock(props)
    return props.open ? <div data-testid="audit-drawer" /> : null
  },
}))

let isMobileValue = false
vi.mock('@/shared/hooks/use-mobile', () => ({
  useIsMobile: () => isMobileValue,
}))

import { ActionsPanel } from './ActionsPanel'

describe('ActionsPanel', () => {
  beforeEach(() => {
    auditDrawerMock.mockClear()
    isMobileValue = false
  })

  it('exibe o botao Log quando entityType e fornecido', () => {
    render(<ActionsPanel entityType="conta" entityId="c1" />)

    expect(screen.getByRole('button', { name: /log/i })).toBeInTheDocument()
  })

  it('oculta o botao Log quando entityType e ausente', () => {
    render(<ActionsPanel />)

    expect(screen.queryByRole('button', { name: /^log$/i })).toBeNull()
  })

  it('desabilita o botao Log quando entityId e null', () => {
    render(<ActionsPanel entityType="conta" entityId={null} />)

    expect(screen.getByRole('button', { name: /log/i })).toBeDisabled()
  })

  it('abre o drawer de auditoria ao clicar em Log', async () => {
    render(<ActionsPanel entityType="conta" entityId="c1" />)

    await userEvent.click(screen.getByRole('button', { name: /log/i }))

    expect(screen.getByTestId('audit-drawer')).toBeInTheDocument()
  })

  it('desabilita Exportar CSV quando onExportCsv e ausente', () => {
    render(<ActionsPanel />)

    expect(
      screen.getByRole('button', { name: /exportar csv/i }),
    ).toBeDisabled()
  })

  it('chama onExportCsv ao clicar em Exportar CSV', async () => {
    const onExportCsv = vi.fn()
    render(<ActionsPanel onExportCsv={onExportCsv} />)

    await userEvent.click(
      screen.getByRole('button', { name: /exportar csv/i }),
    )

    expect(onExportCsv).toHaveBeenCalledOnce()
  })

  it('oculta o botao Imprimir quando onPrint e ausente', () => {
    render(<ActionsPanel />)

    expect(screen.queryByRole('button', { name: /imprimir/i })).toBeNull()
  })

  it('chama onPrint ao clicar em Imprimir', async () => {
    const onPrint = vi.fn()
    render(<ActionsPanel onPrint={onPrint} />)

    await userEvent.click(screen.getByRole('button', { name: /imprimir/i }))

    expect(onPrint).toHaveBeenCalledOnce()
  })

  it('renderiza extraActions no painel', () => {
    render(<ActionsPanel extraActions={<button>Acao Extra</button>} />)

    expect(
      screen.getByRole('button', { name: /acao extra/i }),
    ).toBeInTheDocument()
  })

  it('colapsa as acoes num menu em viewport mobile', () => {
    isMobileValue = true
    render(<ActionsPanel onExportCsv={vi.fn()} />)

    expect(screen.getByRole('button', { name: /acoes/i })).toBeInTheDocument()
  })
})
