'use client'
import { useMemo } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { orcamentoService } from '../services/orcamento-service'
import type { Orcamento, Progresso } from '../types/orcamento'

export interface OrcamentoComProgresso {
  orcamento: Orcamento
  progresso: Progresso | undefined
}

function pad2(valor: number): string {
  return valor.toString().padStart(2, '0')
}

function mesAtualYYYYMM(): string {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${pad2(hoje.getMonth() + 1)}`
}

export function useOrcamentosAtivosComProgresso(): {
  itens: OrcamentoComProgresso[]
  isLoading: boolean
} {
  const mesAtual = mesAtualYYYYMM()

  const { data: orcamentos, isLoading: isLoadingLista } = useQuery({
    queryKey: ['orcamentos'],
    queryFn: () => orcamentoService.listar(),
  })

  const ativos = useMemo(
    () =>
      (orcamentos ?? []).filter(
        (o) => o.ativo && (o.mesAno ?? '').startsWith(mesAtual),
      ),
    [orcamentos, mesAtual],
  )

  const progressoQueries = useQueries({
    queries: ativos.map((orcamento) => ({
      queryKey: ['orcamento-progresso', orcamento.id],
      queryFn: () => orcamentoService.progresso(orcamento.id),
      enabled: !!orcamento.id,
    })),
  })

  const isLoadingProgresso = progressoQueries.some((q) => q.isLoading)

  const itens = useMemo<OrcamentoComProgresso[]>(
    () =>
      ativos.map((orcamento, i) => ({
        orcamento,
        progresso: progressoQueries[i]?.data,
      })),
    [ativos, progressoQueries],
  )

  return {
    itens,
    isLoading: isLoadingLista || isLoadingProgresso,
  }
}
