/**
 * Hook que registra handlers globais de erros assincronos
 * (`window.error` e `window.unhandledrejection`) e propaga-os ao
 * `useErrorBannerStore` em forma de banner dismissivel. Cada erro
 * tambem e enviado ao backend (`POST /api/incidentes` via
 * incidenteService) para obter um codigo ERR-XXXXXXXX que e
 * atualizado no banner correspondente.
 *
 * Deve ser chamado UMA VEZ no `DashboardShell` -- multiplas chamadas
 * registrariam handlers duplicados.
 */
'use client'

import { useEffect } from 'react'
import { incidenteService } from '@/features/incidente/services/incidente-service'
import { useErrorBannerStore } from '@/shared/shell/error-banner-store'

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
        incidenteService
          .registrar({ operacao, classeErro: tipo, mensagem })
          .then((codigo) => {
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
        incidenteService
          .registrar({ operacao, classeErro: tipo, mensagem })
          .then((codigo) => {
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
