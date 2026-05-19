import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useQuery } from '@tanstack/react-query'
import { LookupField, type LookupOption } from './LookupField'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
}))

const mockUseQuery = vi.mocked(useQuery)

const options: LookupOption[] = [
  { value: '1', label: 'Conta Corrente' },
  { value: '2', label: 'Poupanca' },
  { value: '3', label: 'Carteira' },
]

function setQueryResult(result: { data?: LookupOption[]; isLoading?: boolean }) {
  mockUseQuery.mockReturnValue({
    data: result.data,
    isLoading: result.isLoading ?? false,
  } as unknown as ReturnType<typeof useQuery>)
}

function renderField(props?: Partial<Parameters<typeof LookupField>[0]>) {
  const onChange = vi.fn()
  render(
    <LookupField
      value={null}
      onChange={onChange}
      queryKey={['lookup']}
      queryFn={async () => options}
      {...props}
    />,
  )
  return { onChange }
}

describe('LookupField', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setQueryResult({ data: options })
  })

  it('renderiza o placeholder quando value e null', () => {
    renderField({ value: null, placeholder: 'Selecione a conta' })
    expect(
      screen.getByRole('button', { name: /selecione a conta/i }),
    ).toBeInTheDocument()
  })

  it('renderiza o label da opcao selecionada quando value e preenchido', () => {
    renderField({ value: '2' })
    expect(
      screen.getByRole('button', { name: /poupanca/i }),
    ).toBeInTheDocument()
  })

  it('abre o popover ao clicar no trigger', async () => {
    renderField()
    await userEvent.click(screen.getByRole('button'))
    expect(screen.getByPlaceholderText('Buscar...')).toBeInTheDocument()
  })

  it('filtra as opcoes pelo texto digitado no input de busca', async () => {
    renderField()
    await userEvent.click(screen.getByRole('button'))
    await userEvent.type(screen.getByPlaceholderText('Buscar...'), 'poup')

    expect(screen.getByText('Poupanca')).toBeInTheDocument()
    expect(screen.queryByText('Conta Corrente')).not.toBeInTheDocument()
    expect(screen.queryByText('Carteira')).not.toBeInTheDocument()
  })

  it('chama onChange com o value correto ao selecionar uma opcao', async () => {
    const { onChange } = renderField()
    await userEvent.click(screen.getByRole('button'))
    await userEvent.click(screen.getByText('Carteira'))

    expect(onChange).toHaveBeenCalledWith('3')
  })

  it('fecha o popover e limpa a busca apos a selecao', async () => {
    renderField()
    await userEvent.click(screen.getByRole('button'))
    await userEvent.type(screen.getByPlaceholderText('Buscar...'), 'corr')
    await userEvent.click(screen.getByText('Conta Corrente'))

    expect(screen.queryByPlaceholderText('Buscar...')).not.toBeInTheDocument()
  })

  it('exibe skeletons enquanto isLoading e true', async () => {
    setQueryResult({ data: [], isLoading: true })
    render(
      <LookupField
        value={null}
        onChange={vi.fn()}
        queryKey={['lookup']}
        queryFn={async () => options}
      />,
    )
    await userEvent.click(screen.getByRole('button'))

    expect(
      document.body.querySelectorAll('[data-slot="skeleton"]').length,
    ).toBe(3)
  })

  it('exibe a emptyMessage quando a lista filtrada esta vazia', async () => {
    renderField({ emptyMessage: 'Nada aqui.' })
    await userEvent.click(screen.getByRole('button'))
    await userEvent.type(screen.getByPlaceholderText('Buscar...'), 'inexistente')

    expect(screen.getByText('Nada aqui.')).toBeInTheDocument()
  })

  it('nao exibe o botao "Limpar selecao" quando value e null', async () => {
    renderField({ value: null })
    await userEvent.click(screen.getByRole('button'))

    expect(
      screen.queryByRole('button', { name: /limpar selecao/i }),
    ).not.toBeInTheDocument()
  })

  it('exibe o botao "Limpar selecao" quando ha valor selecionado', async () => {
    renderField({ value: '1' })
    await userEvent.click(screen.getByRole('button', { name: /conta corrente/i }))

    expect(
      screen.getByRole('button', { name: /limpar selecao/i }),
    ).toBeInTheDocument()
  })

  it('chama onChange(null) ao clicar em "Limpar selecao"', async () => {
    const { onChange } = renderField({ value: '1' })
    await userEvent.click(screen.getByRole('button', { name: /conta corrente/i }))
    await userEvent.click(
      screen.getByRole('button', { name: /limpar selecao/i }),
    )

    expect(onChange).toHaveBeenCalledWith(null)
  })
})
