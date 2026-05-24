'use client'
/**
 * useNotificacoes -- agrega alertas de orcamentos e metas em uma lista
 * unica de notificacoes para exibicao no shell (toast e badge).
 *
 * Quatro tipos suportados:
 *   1. orcamento_atencao  -- status ATENCAO (>= 80% do limite)
 *   2. orcamento_excedido -- status EXCEDIDO (> 100%)
 *   3. meta_vencendo      -- meta EM_ANDAMENTO com prazo em <= 7 dias
 *   4. meta_vencida       -- meta EM_ANDAMENTO com prazo < hoje
 *
 * Compoe hooks de leitura de cada feature (ADR-013: isolamento de bounded
 * context) -- nao importa services de orcamentos/metas diretamente.
 */
import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useOrcamentosAtivosComProgresso } from '@/features/orcamentos/hooks/useOrcamentosAtivosComProgresso'
import { useMetasEmAndamento } from '@/features/metas/hooks/useMetasEmAndamento'
import { categoriasService } from '@/features/categorias/services/categorias.service'

export type TipoNotificacao =
  | 'orcamento_atencao'
  | 'orcamento_excedido'
  | 'meta_vencendo'
  | 'meta_vencida'

export interface Notificacao {
  id: string
  tipo: TipoNotificacao
  titulo: string
  descricao: string
}

function diasAte(data: Date, hoje: Date): number {
  const MS_POR_DIA = 1000 * 60 * 60 * 24
  const a = new Date(data.getFullYear(), data.getMonth(), data.getDate())
  const b = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate())
  return Math.round((a.getTime() - b.getTime()) / MS_POR_DIA)
}

export function useNotificacoes(): {
  notificacoes: Notificacao[]
  isLoading: boolean
} {
  const { itens: orcamentosComProgresso, isLoading: isLoadingOrcamentos } =
    useOrcamentosAtivosComProgresso()
  const { metas, isLoading: isLoadingMetas } = useMetasEmAndamento()

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: () => categoriasService.listar(),
  })

  const notificacoes = useMemo<Notificacao[]>(() => {
    const lista: Notificacao[] = []

    const nomePorCategoriaId = new Map<string, string>()
    for (const c of categorias ?? []) {
      nomePorCategoriaId.set(c.id, c.nome)
    }

    // Orcamentos
    for (const { orcamento, progresso } of orcamentosComProgresso) {
      if (!progresso) continue

      const nome =
        nomePorCategoriaId.get(orcamento.categoriaId) ?? 'Orcamento'
      const percentual = Math.round(progresso.percentualUtilizado)

      if (progresso.status === 'EXCEDIDO') {
        lista.push({
          id: `orcamento-excedido:${orcamento.id}`,
          tipo: 'orcamento_excedido',
          titulo: 'Orcamento excedido',
          descricao: `${nome}: ${percentual}% utilizado`,
        })
      } else if (progresso.status === 'ATENCAO') {
        lista.push({
          id: `orcamento-atencao:${orcamento.id}`,
          tipo: 'orcamento_atencao',
          titulo: 'Orcamento em atencao',
          descricao: `${nome}: ${percentual}% utilizado`,
        })
      }
    }

    // Metas
    const hoje = new Date()
    for (const meta of metas) {
      if (!meta.prazo) continue

      const partes = meta.prazo.slice(0, 10).split('-')
      if (partes.length !== 3) continue
      const ano = Number(partes[0])
      const mes = Number(partes[1])
      const dia = Number(partes[2])
      if (!ano || !mes || !dia) continue
      const dataPrazo = new Date(ano, mes - 1, dia)

      const dias = diasAte(dataPrazo, hoje)

      if (dias < 0) {
        lista.push({
          id: `meta-vencida:${meta.id}`,
          tipo: 'meta_vencida',
          titulo: 'Meta vencida',
          descricao: `${meta.nome}: prazo encerrado`,
        })
      } else if (dias <= 7) {
        const sufixo =
          dias === 0
            ? 'vence hoje'
            : dias === 1
              ? 'vence em 1 dia'
              : `vence em ${dias} dias`
        lista.push({
          id: `meta-vencendo:${meta.id}`,
          tipo: 'meta_vencendo',
          titulo: 'Meta vencendo em breve',
          descricao: `${meta.nome}: ${sufixo}`,
        })
      }
    }

    return lista
  }, [orcamentosComProgresso, metas, categorias])

  return {
    notificacoes,
    isLoading: isLoadingOrcamentos || isLoadingMetas,
  }
}
