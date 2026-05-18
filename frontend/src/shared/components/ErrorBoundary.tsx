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
    this.state = { hasError: false, codigoErro: null }
  }

  static getDerivedStateFromError(): Partial<ErrorBoundaryState> {
    return { hasError: true }
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
    this.setState({ hasError: false, codigoErro: null })
  }

  render(): ReactNode {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <div className="flex min-h-[50vh] items-center justify-center p-6">
        <div className="w-full max-w-md rounded-xl bg-card p-6 text-card-foreground ring-1 ring-foreground/10">
          <h2 className="text-lg font-semibold">Erro inesperado</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Algo deu errado. Se o problema persistir, informe ao suporte o codigo abaixo.
          </p>
          <p className="mt-4 rounded-md bg-muted px-3 py-2 text-center font-mono text-sm font-medium">
            {this.state.codigoErro ?? 'registrando...'}
          </p>
          <button
            type="button"
            onClick={this.handleRetry}
            className="mt-4 w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Tentar novamente
          </button>
        </div>
      </div>
    )
  }
}
