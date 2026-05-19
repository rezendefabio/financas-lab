import { describe, it, expect, vi, beforeEach } from 'vitest'
import { auditlogService } from './auditlog-service'
import type { AuditLogPage } from '../types/auditlog'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'

const paginaVazia: AuditLogPage = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
}

describe('auditlogService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('listarPorEntidade monta a URL com entityType, entityId, page e size', async () => {
    vi.mocked(apiFetch).mockResolvedValue(paginaVazia)

    await auditlogService.listarPorEntidade('conta', 'id-123', 2, 50)

    expect(apiFetch).toHaveBeenCalledWith(
      '/api/audit-log?entityType=conta&entityId=id-123&page=2&size=50',
    )
  })

  it('listarPorEntidade usa page=0 e size=20 como padrao', async () => {
    vi.mocked(apiFetch).mockResolvedValue(paginaVazia)

    await auditlogService.listarPorEntidade('categoria', 'cat-9')

    expect(apiFetch).toHaveBeenCalledWith(
      '/api/audit-log?entityType=categoria&entityId=cat-9&page=0&size=20',
    )
  })

  it('listarPorEntidade retorna a pagina recebida do apiFetch', async () => {
    vi.mocked(apiFetch).mockResolvedValue(paginaVazia)

    const resultado = await auditlogService.listarPorEntidade('conta', 'id-1')

    expect(resultado).toEqual(paginaVazia)
  })
})
