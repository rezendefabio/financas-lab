/**
 * Hook que registra handlers globais de erros assincronos
 * (`window.error` e `window.unhandledrejection`) e propaga-os ao
 * `useErrorBannerStore` em forma de banner dismissivel. Cada erro
 * tambem e enviado ao backend (`POST /api/incidentes`) para obter um
 * codigo ERR-XXXXXXXX que e atualizado no banner correspondente.
 *
 * Deve ser chamado UMA VEZ no `DashboardShell` -- multiplas chamadas
 * registrariam handlers duplicados.
 */
'use client'

import { useEffect } from 'react'
import { useErrorBannerStore } from '@/shared/shell/error-banner-store'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

async function registrarIncidente(
  tipo: string,
  mensagem: string,
  operacao: string,
): Promise<string | null> {
  try {
    // eslint-disable-next-line no-restricted-globals
    const res = await fetch(`${API_BASE}/api/incidentes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        operacao,
        classeErro: tipo,
        mensagem: mensagem.slice(0, 500),
        stackTrace: '',
      }),
    })
    if (!res.ok) return null
    const data = (await res.json()) as { codigo?: string }
    return data.codigo ?? null
  } catch {
    return null
  }
}

export function useGlobalErrorHandler(): void {
  useEffect(() => {
    const handleError = (event: ErrorEvent) => {
      if (event.filename && !event.filename.includes(window.location.origin)) {
        return
      }

      const { addBanner, updateCodigo } = useErrorBannerStore.getState()
      const operacao = `CLIENT ${window.location.pathname}`
      const mensagem = event.message ?? 'Erro desconhecido'
      const tipo = event.error?.name ?? 'Error'

      addBanner({
        codigo: null,
        mensagem,
        tipo,
        criadoEm: new Date().toISOString(),
      })
      const bannerId = useErrorBannerStore.getState().banners.at(-1)?.id

      if (bannerId) {
        const id = bannerId
        registrarIncidente(tipo, mensagem, operacao).then((codigo) => {
          if (codigo) updateCodigo(id, codigo)
        })
      }
    }

    const handleRejection = (event: PromiseRejectionEvent) => {
      const { addBanner, updateCodigo } = useErrorBannerStore.getState()
      const operacao = `CLIENT ${window.location.pathname}`
      const reason = event.reason
      const mensagem =
        reason instanceof Error
          ? reason.message
          : typeof reason === 'string'
            ? reason
            : 'Promise rejeitada sem mensagem'
      const tipo = reason instanceof Error ? reason.name : 'UnhandledRejection'

      addBanner({
        codigo: null,
        mensagem,
        tipo,
        criadoEm: new Date().toISOString(),
      })
      const bannerId = useErrorBannerStore.getState().banners.at(-1)?.id

      if (bannerId) {
        const id = bannerId
        registrarIncidente(tipo, mensagem, operacao).then((codigo) => {
          if (codigo) updateCodigo(id, codigo)
        })
      }
    }

    window.addEventListener('error', handleError)
    window.addEventListener('unhandledrejection', handleRejection)
    return () => {
      window.removeEventListener('error', handleError)
      window.removeEventListener('unhandledrejection', handleRejection)
    }
  }, [])
}
