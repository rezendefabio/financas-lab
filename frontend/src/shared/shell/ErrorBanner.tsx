/**
 * Banner global de erros assincronos (sub-etapa UI-14). Renderiza uma
 * lista de alertas dismissiveis entre o ShellHeader e o TabBar quando
 * o `useErrorBannerStore` contem banners. Cada item exibe o tipo, a
 * mensagem truncada, o codigo ERR copiavel e o horario do erro.
 */
'use client'

import { useErrorBannerStore, type ErrorBannerItem } from './error-banner-store'

const MAX_MENSAGEM = 80

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch {
    return ''
  }
}

function BannerItem({ item }: { item: ErrorBannerItem }) {
  const dismiss = useErrorBannerStore((s) => s.dismiss)

  const handleCopy = () => {
    if (item.codigo) {
      navigator.clipboard.writeText(item.codigo).catch(() => undefined)
    }
  }

  const mensagem =
    item.mensagem.length > MAX_MENSAGEM
      ? `${item.mensagem.slice(0, MAX_MENSAGEM)}...`
      : item.mensagem

  return (
    <div
      role="alert"
      className="flex items-start gap-3 border-b border-destructive/20 bg-destructive/5 px-4 py-2.5 text-sm"
    >
      <span className="mt-0.5 shrink-0 text-destructive" aria-hidden>
        !
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-baseline gap-2">
          <span className="font-medium text-destructive">{item.tipo}</span>
          <span className="text-xs text-muted-foreground">
            {formatTime(item.criadoEm)}
          </span>
        </div>
        <p className="truncate text-muted-foreground">{mensagem}</p>
        <div className="mt-1 flex items-center gap-2">
          <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-xs font-medium tracking-wider">
            {item.codigo ?? 'registrando...'}
          </code>
          {item.codigo && (
            <button
              type="button"
              onClick={handleCopy}
              className="text-xs text-muted-foreground underline hover:text-foreground"
            >
              copiar
            </button>
          )}
        </div>
      </div>
      <button
        type="button"
        aria-label="Fechar alerta"
        onClick={() => dismiss(item.id)}
        className="shrink-0 rounded p-0.5 text-muted-foreground hover:text-foreground"
      >
        X
      </button>
    </div>
  )
}

export function ErrorBanner() {
  const banners = useErrorBannerStore((s) => s.banners)
  const dismissAll = useErrorBannerStore((s) => s.dismissAll)

  if (banners.length === 0) return null

  return (
    <div role="region" aria-label="Alertas de erro">
      {banners.map((item) => (
        <BannerItem key={item.id} item={item} />
      ))}
      {banners.length > 1 && (
        <div className="flex justify-end border-b border-destructive/20 bg-destructive/5 px-4 py-1">
          <button
            type="button"
            onClick={dismissAll}
            className="text-xs text-muted-foreground underline hover:text-foreground"
          >
            Fechar todos
          </button>
        </div>
      )}
    </div>
  )
}
