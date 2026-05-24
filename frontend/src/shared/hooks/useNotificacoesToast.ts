'use client'
/**
 * useNotificacoesToast -- exibe toast Sonner para cada notificacao calculada
 * por `useNotificacoes`, deduplicando por `id` via `useRef<Set>` para nao
 * spammar a tela em rerenders.
 *
 * Toast `error` para tipos criticos (orcamento_excedido, meta_vencida);
 * `warning` para alertas suaves (orcamento_atencao, meta_vencendo).
 */
import { useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { useNotificacoes } from './useNotificacoes'

export function useNotificacoesToast(): void {
  const { notificacoes, isLoading } = useNotificacoes()
  const exibidosRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    if (isLoading || notificacoes.length === 0) return

    for (const n of notificacoes) {
      if (exibidosRef.current.has(n.id)) continue
      exibidosRef.current.add(n.id)

      if (n.tipo === 'orcamento_excedido' || n.tipo === 'meta_vencida') {
        toast.error(n.titulo, { description: n.descricao })
      } else {
        toast.warning(n.titulo, { description: n.descricao })
      }
    }
  }, [notificacoes, isLoading])
}
