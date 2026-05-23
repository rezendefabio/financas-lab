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
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ codigo: 'ERR-ABCD1234' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Erro inesperado na pagina')).toBeInTheDocument()
    expect(await screen.findByText('ERR-ABCD1234')).toBeInTheDocument()

    const fetchCall = fetchMock.mock.calls[0]
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

    expect(screen.getByText('Erro inesperado na pagina')).toBeInTheDocument()
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

    expect(screen.getByText('Erro inesperado na pagina')).toBeInTheDocument()

    deveExplodir = false
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(screen.getByText('conteudo recuperado')).toBeInTheDocument()
  })

  it('copia o codigo ERR para o clipboard e mostra feedback "Copiado!"', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })
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

    await screen.findByText('ERR-ABCD1234')

    await userEvent.click(screen.getByRole('button', { name: 'Copiar' }))

    expect(writeText).toHaveBeenCalledWith('ERR-ABCD1234')
    expect(await screen.findByRole('button', { name: 'Copiado!' })).toBeInTheDocument()
  })

  it('chama window.history.back ao clicar em "Voltar"', async () => {
    const backSpy = vi.spyOn(window.history, 'back').mockImplementation(() => {})
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

    await screen.findByText('ERR-ABCD1234')
    await userEvent.click(screen.getByRole('button', { name: 'Voltar' }))

    expect(backSpy).toHaveBeenCalledTimes(1)
  })

  it('mostra a mensagem de erro tecnica dentro do bloco "Detalhe tecnico"', () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

    render(
      <ErrorBoundary>
        <Bomba />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Detalhe tecnico')).toBeInTheDocument()
    expect(screen.getByText('falha de renderizacao')).toBeInTheDocument()
  })
})
