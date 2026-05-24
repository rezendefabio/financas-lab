'use client'
import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { metaService } from '../services/meta-service'
import type { Meta } from '../types/meta'

export function useMetasEmAndamento(): {
  metas: Meta[]
  isLoading: boolean
} {
  const { data, isLoading } = useQuery({
    queryKey: ['metas'],
    queryFn: () => metaService.listar(),
  })

  const metas = useMemo(
    () => (data ?? []).filter((m) => m.status === 'EM_ANDAMENTO'),
    [data],
  )

  return { metas, isLoading }
}
