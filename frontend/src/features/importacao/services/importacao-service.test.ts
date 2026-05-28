import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({
  apiFetchMultipart: vi.fn(),
  apiFetchBlob: vi.fn(),
}))

import { apiFetchMultipart, apiFetchBlob } from '@/services/api-client'
import { importacaoService } from './importacao-service'
import type {
  AnaliseImportacaoResponse,
  ImportacaoJobResponse,
} from '../types/importacao'

const mockJob: ImportacaoJobResponse = { jobExecutionId: 42 }

const mockAnalise: AnaliseImportacaoResponse = {
  totalLinhas: 1,
  linhasValidas: 1,
  possivelDuplicatas: 0,
  errosParsing: 0,
  itens: [],
  erros: [],
}

afterEach(() => {
  vi.clearAllMocks()
  vi.restoreAllMocks()
})

describe('importacaoService', () => {
  it('importarCsv() chama apiFetchMultipart com o arquivo no endpoint do job', async () => {
    vi.mocked(apiFetchMultipart).mockResolvedValue(mockJob)
    const file = new File(['conteudo'], 'extrato.csv', { type: 'text/csv' })

    const result = await importacaoService.importarCsv(file)

    expect(apiFetchMultipart).toHaveBeenCalledTimes(1)
    const [path, formData] = vi.mocked(apiFetchMultipart).mock.calls[0]
    expect(path).toBe('/api/jobs/importacao-csv-transacoes')
    expect(formData).toBeInstanceOf(FormData)
    expect((formData as FormData).get('arquivo')).toBe(file)
    expect(result).toEqual(mockJob)
  })

  it('analisarCsv() chama apiFetchMultipart no endpoint de analise', async () => {
    vi.mocked(apiFetchMultipart).mockResolvedValue(mockAnalise)
    const file = new File(['conteudo'], 'extrato.csv', { type: 'text/csv' })

    const result = await importacaoService.analisarCsv(file)

    expect(apiFetchMultipart).toHaveBeenCalledTimes(1)
    const [path, formData] = vi.mocked(apiFetchMultipart).mock.calls[0]
    expect(path).toBe('/api/importacoes/analisar')
    expect(formData).toBeInstanceOf(FormData)
    expect((formData as FormData).get('arquivo')).toBe(file)
    expect(result).toEqual(mockAnalise)
  })

  it('downloadModelo() baixa o blob e dispara o download via link temporario', async () => {
    const blob = new Blob(['csv'], { type: 'text/csv' })
    vi.mocked(apiFetchBlob).mockResolvedValue(blob)

    const createObjectURL = vi.fn(() => 'blob:modelo')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })

    const click = vi.fn()
    const anchor = { href: '', download: '', click } as unknown as HTMLAnchorElement
    const createElement = vi
      .spyOn(document, 'createElement')
      .mockReturnValue(anchor)

    await importacaoService.downloadModelo()

    expect(apiFetchBlob).toHaveBeenCalledWith(
      '/api/jobs/importacao-csv-transacoes/csv/modelo',
    )
    expect(createObjectURL).toHaveBeenCalledWith(blob)
    expect(createElement).toHaveBeenCalledWith('a')
    expect(anchor.href).toBe('blob:modelo')
    expect(anchor.download).toBe('modelo-importacao-transacoes.csv')
    expect(click).toHaveBeenCalledTimes(1)
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:modelo')
  })
})
