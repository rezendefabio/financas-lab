import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { AnaliseImportacaoResponse } from '@/features/importacao'

vi.mock('@/features/importacao', () => ({
  importacaoService: {
    importarCsv: vi.fn(),
    analisarCsv: vi.fn(),
    downloadModelo: vi.fn(),
  },
}))

import ImportacaoPage from './page'
import { importacaoService } from '@/features/importacao'

function makeArquivoCsv(): File {
  return new File(['header\nlinha'], 'transacoes.csv', {
    type: 'text/csv',
  })
}

const CONTA_A = '11111111-0000-0000-0000-000000000001'
const CONTA_B = '22222222-0000-0000-0000-000000000002'

function analiseMock(
  overrides: Partial<AnaliseImportacaoResponse> = {},
): AnaliseImportacaoResponse {
  return {
    totalLinhas: 2,
    linhasValidas: 2,
    possivelDuplicatas: 1,
    errosParsing: 0,
    itens: [
      {
        linha: 2,
        linhaCsvOriginal: `DESPESA,150.00,BRL,2026-05-01,Supermercado,${CONTA_A},,`,
        tipo: 'DESPESA',
        valor: 150,
        moeda: 'BRL',
        data: '2026-05-01',
        descricao: 'Supermercado',
        contaId: CONTA_A,
        possivelDuplicata: true,
        transacaoExistenteId: 'aaaa1111-0000-0000-0000-000000000001',
      },
      {
        linha: 3,
        linhaCsvOriginal: `RECEITA,3000.00,BRL,2026-05-01,Salario,${CONTA_B},,`,
        tipo: 'RECEITA',
        valor: 3000,
        moeda: 'BRL',
        data: '2026-05-01',
        descricao: 'Salario',
        contaId: CONTA_B,
        possivelDuplicata: false,
        transacaoExistenteId: null,
      },
    ],
    erros: [],
    ...overrides,
  }
}

describe('ImportacaoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza o titulo e o subtitulo da pagina', () => {
    render(<ImportacaoPage />)
    expect(
      screen.getByRole('heading', { name: /importar transacoes/i }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/faca o upload de um arquivo csv/i),
    ).toBeInTheDocument()
  })

  it('mantem o botao Analisar desabilitado sem arquivo', () => {
    render(<ImportacaoPage />)
    expect(screen.getByRole('button', { name: 'Analisar' })).toBeDisabled()
  })

  it('apos upload, chama analisarCsv e exibe tabela de revisao', async () => {
    vi.mocked(importacaoService.analisarCsv).mockResolvedValue(analiseMock())
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Analisar' }))

    await waitFor(() => {
      expect(
        screen.getByText(/revise as linhas antes de importar/i),
      ).toBeInTheDocument()
    })
    expect(screen.getByText('Supermercado')).toBeInTheDocument()
    expect(screen.getByText('Salario')).toBeInTheDocument()
    expect(importacaoService.analisarCsv).toHaveBeenCalledTimes(1)
  })

  it('marca linha com possivelDuplicata=true com badge Possivel duplicata', async () => {
    vi.mocked(importacaoService.analisarCsv).mockResolvedValue(analiseMock())
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Analisar' }))

    await waitFor(() => {
      expect(screen.getByText('Possivel duplicata')).toBeInTheDocument()
    })
    expect(screen.getByText('OK')).toBeInTheDocument()
  })

  it('permite desmarcar uma linha e reflete contagem no botao', async () => {
    vi.mocked(importacaoService.analisarCsv).mockResolvedValue(analiseMock())
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Analisar' }))

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /importar selecionadas \(2\)/i }),
      ).toBeInTheDocument()
    })

    const checkboxLinha2 = screen.getByLabelText('Selecionar linha 2')
    await user.click(checkboxLinha2)

    expect(
      screen.getByRole('button', { name: /importar selecionadas \(1\)/i }),
    ).toBeInTheDocument()
  })

  it('importa apenas linhas selecionadas reconstruindo o CSV', async () => {
    vi.mocked(importacaoService.analisarCsv).mockResolvedValue(analiseMock())
    vi.mocked(importacaoService.importarCsv).mockResolvedValue({
      jobExecutionId: 99,
      status: 'STARTED',
    })

    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Analisar' }))

    await waitFor(() => {
      expect(screen.getByLabelText('Selecionar linha 2')).toBeInTheDocument()
    })

    // desmarcar linha 2 (duplicata)
    await user.click(screen.getByLabelText('Selecionar linha 2'))

    await user.click(
      screen.getByRole('button', { name: /importar selecionadas \(1\)/i }),
    )

    await waitFor(() => {
      expect(importacaoService.importarCsv).toHaveBeenCalledTimes(1)
    })

    const arquivoEnviado = vi.mocked(importacaoService.importarCsv).mock
      .calls[0][0]
    const conteudo = await arquivoEnviado.text()
    expect(conteudo).toContain(
      'tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId',
    )
    expect(conteudo).toContain('Salario')
    expect(conteudo).not.toContain('Supermercado')

    await waitFor(() => {
      expect(
        screen.getByText(/importacao iniciada\. job id: 99/i),
      ).toBeInTheDocument()
    })
  })

  it('exibe o botao Baixar modelo CSV na etapa de upload', () => {
    render(<ImportacaoPage />)
    expect(
      screen.getByRole('button', { name: 'Baixar modelo CSV' }),
    ).toBeInTheDocument()
  })

  it('chama downloadModelo ao clicar no botao Baixar modelo CSV', async () => {
    vi.mocked(importacaoService.downloadModelo).mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.click(
      screen.getByRole('button', { name: 'Baixar modelo CSV' }),
    )

    expect(importacaoService.downloadModelo).toHaveBeenCalledTimes(1)
  })

  it('botao Voltar retorna para etapa de upload', async () => {
    vi.mocked(importacaoService.analisarCsv).mockResolvedValue(analiseMock())
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Analisar' }))

    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Voltar' }),
      ).toBeInTheDocument(),
    )

    await user.click(screen.getByRole('button', { name: 'Voltar' }))

    expect(
      screen.getByRole('button', { name: 'Analisar' }),
    ).toBeInTheDocument()
  })
})
