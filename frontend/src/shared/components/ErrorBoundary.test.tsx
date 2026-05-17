import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ErrorBoundary } from './ErrorBoundary'

let deveExplodir = true

function Bomba() {
  if (deveExplodir) {
    throw new Error('falha de renderizacao')
  }
  return <div>conteudo recuperado</div>
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    deveExplodir = true
    // React imprime o erro capturado no console; silenciado para nao poluir o output.
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('renderiza os filhos quando nao ha erro', () => {
    deveExplodir = false
    vi.stubGlobal('fetch', vi.fn())

    render(
      <ErrorBoundary>
        <div>conteudo normal</div>
      </ErrorBoundary>,
    )

    expect(screen.getByText('conteudo normal')).toBeInTheDocument()
  })

  it('exibe o fallback e registra o incidente quando um filho lanca erro', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ codigo: 'ERR-ABCD1234' }),
      }),
    )

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Erro inesperado')).toBeInTheDocument()
    expect(await screen.findByText('ERR-ABCD1234')).toBeInTheDocument()

    const fetchCall = vi.mocked(fetch).mock.calls[0]
    expect(fetchCall[0]).toContain('/api/incidentes')
    expect((fetchCall[1] as RequestInit)?.method).toBe('POST')
    const payload = JSON.parse((fetchCall[1] as RequestInit)?.body as string)
    expect(payload.classeErro).toBe('Error')
    expect(payload.mensagem).toBe('falha de renderizacao')
    expect(payload.operacao).toContain('CLIENT ')
  })

  it('mostra "registrando..." enquanto o codigo nao chegou', () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('registrando...')).toBeInTheDocument()
  })

  it('nao quebra quando o registro do incidente falha', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('rede indisponivel')))

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Erro inesperado')).toBeInTheDocument()
    expect(screen.getByText('registrando...')).toBeInTheDocument()
  })

  it('volta a renderizar os filhos ao clicar em "Tentar novamente"', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ codigo: 'ERR-ABCD1234' }),
      }),
    )

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Erro inesperado')).toBeInTheDocument()

    deveExplodir = false
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(screen.getByText('conteudo recuperado')).toBeInTheDocument()
  })
})
