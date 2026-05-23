'use client'

import { Component, type ErrorInfo, type ReactNode } from 'react'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'
const MAX_STACK_TRACE = 2000

interface ErrorBoundaryProps {
  children: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  codigoErro: string | null
  mensagemErro: string | null
  copiadoMsg: boolean
}

/**
 * Captura erros de renderizacao do React, registra o incidente no backend
 * (`POST /api/incidentes`) e exibe o codigo de erro para o usuario informar
 * ao suporte. A chamada de registro usa `fetch` direto -- nao o api-client --
 * porque pode ocorrer antes de o token de autenticacao estar disponivel.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, codigoErro: null, mensagemErro: null, copiadoMsg: false }
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, mensagemErro: error.message }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    const payload = {
      operacao: `CLIENT ${typeof window !== 'undefined' ? window.location.pathname : ''}`,
      classeErro: error.name,
      mensagem: error.message,
      stackTrace: (info.componentStack ?? '').slice(0, MAX_STACK_TRACE),
    }

    // Excecao deliberada a regra no-restricted-globals: o registro nao passa
    // pelo api-client porque pode ocorrer antes de o token estar disponivel.
    // eslint-disable-next-line no-restricted-globals
    fetch(`${API_BASE}/api/incidentes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data: { codigo?: string } | null) => {
        if (data?.codigo) {
          this.setState({ codigoErro: data.codigo })
        }
      })
      .catch(() => {
        // Registro best-effort: falha ao registrar nao deve quebrar o boundary.
      })
  }

  private handleRetry = (): void => {
    this.setState({ hasError: false, codigoErro: null, mensagemErro: null, copiadoMsg: false })
  }

  private handleCopy = (): void => {
    if (!this.state.codigoErro) return
    navigator.clipboard.writeText(this.state.codigoErro).then(() => {
      this.setState({ copiadoMsg: true })
      setTimeout(() => this.setState({ copiadoMsg: false }), 2000)
    }).catch(() => undefined)
  }

  render(): ReactNode {
    if (!this.state.hasError) {
      return this.props.children
    }

    const { codigoErro, mensagemErro, copiadoMsg } = this.state

    return (
      <div className="p-6">
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 space-y-4">
          <div className="flex items-start gap-3">
            <span className="text-destructive text-xl leading-none mt-0.5" aria-hidden>⚠</span>
            <div className="flex-1 space-y-1">
              <h2 className="font-semibold text-destructive">Erro inesperado na pagina</h2>
              <p className="text-sm text-muted-foreground">
                Algo deu errado ao carregar este conteudo. Informe ao suporte o codigo abaixo.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <code className="flex-1 rounded-md bg-muted px-3 py-2 font-mono text-sm font-medium tracking-wider">
              {codigoErro ?? 'registrando...'}
            </code>
            {codigoErro && (
              <button
                type="button"
                onClick={this.handleCopy}
                className="rounded-md border border-border px-3 py-2 text-xs font-medium hover:bg-muted transition-colors"
              >
                {copiadoMsg ? 'Copiado!' : 'Copiar'}
              </button>
            )}
          </div>

          {mensagemErro && (
            <details className="text-xs text-muted-foreground">
              <summary className="cursor-pointer select-none hover:text-foreground">
                Detalhe tecnico
              </summary>
              <p className="mt-1 rounded bg-muted px-2 py-1 font-mono break-all">
                {mensagemErro}
              </p>
            </details>
          )}

          <div className="flex gap-2">
            <button
              type="button"
              onClick={this.handleRetry}
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Tentar novamente
            </button>
            <button
              type="button"
              onClick={() => window.history.back()}
              className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-muted"
            >
              Voltar
            </button>
          </div>
        </div>
      </div>
    )
  }
}
