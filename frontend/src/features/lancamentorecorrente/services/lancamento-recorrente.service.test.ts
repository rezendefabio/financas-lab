import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'
import { lancamentoRecorrenteService } from './lancamento-recorrente.service'
import type { LancamentoRecorrente, CriarLancamentoRecorrenteRequest } from '../types/lancamento-recorrente'

const lancamentoFixture = (overrides?: Partial<LancamentoRecorrente>): LancamentoRecorrente => ({
  id: 'lr-123',
  descricao: 'Aluguel',
  tipo: 'DESPESA',
  valor: { valor: 1200, moeda: 'BRL' },
  contaId: 'conta-abc',
  categoriaId: null,
  periodicidade: 'MENSAL',
  proximaOcorrencia: '2026-06-01',
  ativo: true,
  criadoEm: '2026-05-01T00:00:00Z',
  atualizadoEm: '2026-05-01T00:00:00Z',
  ...overrides,
})

describe('lancamentoRecorrenteService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listar', () => {
    it('chama GET /api/lancamentos-recorrentes', async () => {
      const lista = [lancamentoFixture()]
      vi.mocked(apiFetch).mockResolvedValue(lista)

      const result = await lancamentoRecorrenteService.listar()

      expect(apiFetch).toHaveBeenCalledWith('/api/lancamentos-recorrentes')
      expect(result).toEqual(lista)
    })
  })

  describe('criar', () => {
    it('chama POST /api/lancamentos-recorrentes com body correto', async () => {
      const request: CriarLancamentoRecorrenteRequest = {
        descricao: 'Aluguel',
        tipo: 'DESPESA',
        valorValor: 1200,
        valorMoeda: 'BRL',
        contaId: 'conta-abc',
        periodicidade: 'MENSAL',
        proximaOcorrencia: '2026-06-01',
      }
      const created = lancamentoFixture()
      vi.mocked(apiFetch).mockResolvedValue(created)

      const result = await lancamentoRecorrenteService.criar(request)

      expect(apiFetch).toHaveBeenCalledWith('/api/lancamentos-recorrentes', {
        method: 'POST',
        body: JSON.stringify(request),
      })
      expect(result).toEqual(created)
    })
  })

  describe('buscar', () => {
    it('chama GET /api/lancamentos-recorrentes/:id', async () => {
      const lancamento = lancamentoFixture()
      vi.mocked(apiFetch).mockResolvedValue(lancamento)

      const result = await lancamentoRecorrenteService.buscar('lr-123')

      expect(apiFetch).toHaveBeenCalledWith('/api/lancamentos-recorrentes/lr-123')
      expect(result).toEqual(lancamento)
    })
  })

  describe('desativar', () => {
    it('chama DELETE /api/lancamentos-recorrentes/:id', async () => {
      vi.mocked(apiFetch).mockResolvedValue(undefined)

      await lancamentoRecorrenteService.desativar('lr-123')

      expect(apiFetch).toHaveBeenCalledWith('/api/lancamentos-recorrentes/lr-123', {
        method: 'DELETE',
      })
    })
  })

  describe('executar', () => {
    it('chama POST /api/lancamentos-recorrentes/:id/execucoes', async () => {
      vi.mocked(apiFetch).mockResolvedValue(undefined)

      await lancamentoRecorrenteService.executar('lr-123')

      expect(apiFetch).toHaveBeenCalledWith('/api/lancamentos-recorrentes/lr-123/execucoes', {
        method: 'POST',
      })
    })
  })
})
