import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import {
  StatusBadge,
  ORCAMENTO_STATUS_CONFIG,
  META_STATUS_CONFIG,
} from './StatusBadge'

const testConfig = {
  ATIVO: { label: 'Ativo', variant: 'default' as const },
  INATIVO: { label: 'Inativo', variant: 'secondary' as const },
}

describe('StatusBadge', () => {
  it('renderiza label e variant corretos para status conhecido', () => {
    render(<StatusBadge status="ATIVO" config={testConfig} />)
    expect(screen.getByText('Ativo')).toBeInTheDocument()
  })

  it('usa fallbackLabel para status desconhecido', () => {
    render(<StatusBadge status="OUTRO" config={testConfig} fallbackLabel="Desconhecido" />)
    expect(screen.getByText('Desconhecido')).toBeInTheDocument()
  })

  it('usa o status como texto quando nao ha fallbackLabel e status e desconhecido', () => {
    render(<StatusBadge status="OUTRO_STATUS" config={testConfig} />)
    expect(screen.getByText('OUTRO_STATUS')).toBeInTheDocument()
  })

  it('ORCAMENTO_STATUS_CONFIG tem todas as 4 entradas esperadas', () => {
    expect(ORCAMENTO_STATUS_CONFIG).toHaveProperty('ABAIXO')
    expect(ORCAMENTO_STATUS_CONFIG).toHaveProperty('ATENCAO')
    expect(ORCAMENTO_STATUS_CONFIG).toHaveProperty('ATINGIDO')
    expect(ORCAMENTO_STATUS_CONFIG).toHaveProperty('EXCEDIDO')
  })

  it('META_STATUS_CONFIG tem as 3 entradas esperadas', () => {
    expect(META_STATUS_CONFIG).toHaveProperty('EM_ANDAMENTO')
    expect(META_STATUS_CONFIG).toHaveProperty('CONCLUIDA')
    expect(META_STATUS_CONFIG).toHaveProperty('CANCELADA')
  })
})
