import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { buildCsv, exportToCsv } from './export-csv'

const columns = [
  { key: 'nome', label: 'Nome' },
  { key: 'valor', label: 'Valor' },
]

describe('buildCsv', () => {
  it('gera header a partir dos labels das colunas', () => {
    const csv = buildCsv([], columns)
    expect(csv).toBe('Nome,Valor')
  })

  it('gera uma linha por registro na ordem das colunas', () => {
    const csv = buildCsv([{ nome: 'Conta A', valor: 100 }], columns)
    expect(csv).toBe('Nome,Valor\r\nConta A,100')
  })

  it('escapa valores com virgula envolvendo em aspas duplas', () => {
    const csv = buildCsv([{ nome: 'Silva, Joao', valor: 1 }], columns)
    expect(csv).toContain('"Silva, Joao"')
  })

  it('escapa aspas duplas internas duplicando-as', () => {
    const csv = buildCsv([{ nome: 'Conta "VIP"', valor: 1 }], columns)
    expect(csv).toContain('"Conta ""VIP"""')
  })

  it('escapa quebras de linha envolvendo em aspas', () => {
    const csv = buildCsv([{ nome: 'linha1\nlinha2', valor: 1 }], columns)
    expect(csv).toContain('"linha1\nlinha2"')
  })

  it('trata valores null e undefined como celula vazia', () => {
    const csv = buildCsv([{ nome: null, valor: undefined }], columns)
    expect(csv).toBe('Nome,Valor\r\n,')
  })
})

describe('exportToCsv', () => {
  beforeEach(() => {
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:fake')
    globalThis.URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('dispara o download criando um link com nome de arquivo', () => {
    const clickSpy = vi.fn()
    const realCreate = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = realCreate(tag)
      if (tag === 'a') el.click = clickSpy
      return el
    })

    exportToCsv('contas', [{ nome: 'A', valor: 1 }], columns)

    expect(clickSpy).toHaveBeenCalledOnce()
    expect(globalThis.URL.createObjectURL).toHaveBeenCalledOnce()
    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledOnce()
  })

  it('adiciona a extensao .csv quando ausente', () => {
    let downloadName = ''
    const realCreate = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = realCreate(tag)
      if (tag === 'a') {
        el.click = vi.fn()
        Object.defineProperty(el, 'download', {
          set: (v: string) => {
            downloadName = v
          },
          get: () => downloadName,
        })
      }
      return el
    })

    exportToCsv('relatorio', [], columns)

    expect(downloadName).toBe('relatorio.csv')
  })

  it('mantem a extensao .csv quando ja informada', () => {
    let downloadName = ''
    const realCreate = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = realCreate(tag)
      if (tag === 'a') {
        el.click = vi.fn()
        Object.defineProperty(el, 'download', {
          set: (v: string) => {
            downloadName = v
          },
          get: () => downloadName,
        })
      }
      return el
    })

    exportToCsv('relatorio.csv', [], columns)

    expect(downloadName).toBe('relatorio.csv')
  })
})
