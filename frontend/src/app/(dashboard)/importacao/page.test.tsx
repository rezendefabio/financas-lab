import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/importacao', () => ({
  importacaoService: {
    importarCsv: vi.fn(),
    downloadModelo: vi.fn(),
  },
}))

import ImportacaoPage from './page'
import { importacaoService } from '@/features/importacao'

function makeArquivoCsv(): File {
  return new File(['data,valor\n2026-05-01,10'], 'transacoes.csv', {
    type: 'text/csv',
  })
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

  it('mantem o botao Importar desabilitado enquanto nenhum arquivo e selecionado', () => {
    render(<ImportacaoPage />)
    expect(screen.getByRole('button', { name: 'Importar' })).toBeDisabled()
  })

  it('habilita o botao Importar apos selecionar um arquivo', async () => {
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    const input = screen.getByLabelText('Arquivo')
    await user.upload(input, makeArquivoCsv())

    expect(screen.getByRole('button', { name: 'Importar' })).toBeEnabled()
  })

  it('exibe o job id em caso de sucesso', async () => {
    vi.mocked(importacaoService.importarCsv).mockResolvedValue({
      jobExecutionId: 42,
      status: 'STARTED',
    })
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Importar' }))

    await waitFor(() => {
      expect(
        screen.getByText(/importacao iniciada\. job id: 42/i),
      ).toBeInTheDocument()
    })
    expect(importacaoService.importarCsv).toHaveBeenCalledTimes(1)
  })

  it('exibe mensagem de erro quando a importacao falha', async () => {
    vi.mocked(importacaoService.importarCsv).mockRejectedValue(
      new Error('Arquivo invalido'),
    )
    const user = userEvent.setup()
    render(<ImportacaoPage />)

    await user.upload(screen.getByLabelText('Arquivo'), makeArquivoCsv())
    await user.click(screen.getByRole('button', { name: 'Importar' }))

    await waitFor(() => {
      expect(screen.getByText('Arquivo invalido')).toBeInTheDocument()
    })
  })

  it('exibe o botao Baixar modelo CSV', () => {
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

  it('exibe a tabela de formato com as colunas esperadas', () => {
    render(<ImportacaoPage />)
    expect(screen.getByText('contaId')).toBeInTheDocument()
    expect(screen.getByText('categoriaId')).toBeInTheDocument()
    expect(
      screen.getByRole('columnheader', { name: 'Coluna' }),
    ).toBeInTheDocument()
  })
})
