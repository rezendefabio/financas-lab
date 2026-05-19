import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { DataTable, type ColumnDef } from './DataTable'

interface Row {
  id: string
  nome: string
  valor: number
}

const rows: Row[] = [
  { id: 'r1', nome: 'Conta A', valor: 100 },
  { id: 'r2', nome: 'Conta B', valor: 200 },
]

const columns: ColumnDef<Row>[] = [
  { key: 'nome', label: 'Nome', sortable: true },
  { key: 'valor', label: 'Valor' },
]

describe('DataTable', () => {
  it('renderiza headers e linhas de dados', () => {
    render(<DataTable data={rows} columns={columns} keyField="id" />)

    expect(screen.getByText('Nome')).toBeInTheDocument()
    expect(screen.getByText('Valor')).toBeInTheDocument()
    expect(screen.getByText('Conta A')).toBeInTheDocument()
    expect(screen.getByText('Conta B')).toBeInTheDocument()
  })

  it('usa render customizado da coluna quando fornecido', () => {
    const cols: ColumnDef<Row>[] = [
      { key: 'nome', label: 'Nome' },
      {
        key: 'valor',
        label: 'Valor',
        render: (value) => <span>R$ {String(value)}</span>,
      },
    ]
    render(<DataTable data={rows} columns={cols} keyField="id" />)

    expect(screen.getByText('R$ 100')).toBeInTheDocument()
  })

  it('exibe a mensagem padrao quando nao ha dados', () => {
    render(<DataTable data={[]} columns={columns} keyField="id" />)

    expect(screen.getByText('Nenhum registro encontrado.')).toBeInTheDocument()
  })

  it('exibe a mensagem vazia customizada', () => {
    render(
      <DataTable
        data={[]}
        columns={columns}
        keyField="id"
        emptyMessage="Sem contas."
      />,
    )

    expect(screen.getByText('Sem contas.')).toBeInTheDocument()
  })

  it('renderiza skeletons durante o loading', () => {
    render(
      <DataTable data={[]} columns={columns} keyField="id" isLoading />,
    )

    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('dispara onRowClick ao clicar numa linha', async () => {
    const onRowClick = vi.fn()
    render(
      <DataTable
        data={rows}
        columns={columns}
        keyField="id"
        onRowClick={onRowClick}
      />,
    )

    await userEvent.click(screen.getByText('Conta A'))

    expect(onRowClick).toHaveBeenCalledWith(rows[0])
  })

  it('chama onSortChange ao clicar num header ordenavel', async () => {
    const onSortChange = vi.fn()
    render(
      <DataTable
        data={rows}
        columns={columns}
        keyField="id"
        onSortChange={onSortChange}
      />,
    )

    await userEvent.click(screen.getByText('Nome'))

    expect(onSortChange).toHaveBeenCalledWith('nome', 'asc')
  })

  it('alterna a direcao do sort quando a coluna ja esta ativa em asc', async () => {
    const onSortChange = vi.fn()
    render(
      <DataTable
        data={rows}
        columns={columns}
        keyField="id"
        sort={{ field: 'nome', dir: 'asc' }}
        onSortChange={onSortChange}
      />,
    )

    await userEvent.click(screen.getByText('Nome'))

    expect(onSortChange).toHaveBeenCalledWith('nome', 'desc')
  })

  it('renderiza a coluna Acoes quando rowActions e fornecido', () => {
    render(
      <DataTable
        data={rows}
        columns={columns}
        keyField="id"
        rowActions={(row) => <button>Editar {row.nome}</button>}
      />,
    )

    expect(screen.getByText('Acoes')).toBeInTheDocument()
    expect(screen.getByText('Editar Conta A')).toBeInTheDocument()
  })

  it('nao dispara onRowClick ao clicar dentro da celula de acoes', async () => {
    const onRowClick = vi.fn()
    render(
      <DataTable
        data={rows}
        columns={columns}
        keyField="id"
        onRowClick={onRowClick}
        rowActions={(row) => <button>Acao {row.nome}</button>}
      />,
    )

    await userEvent.click(screen.getByText('Acao Conta A'))

    expect(onRowClick).not.toHaveBeenCalled()
  })
})
