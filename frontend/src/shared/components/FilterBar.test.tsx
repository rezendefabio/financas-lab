import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import {
  FilterBar,
  type ActiveFilter,
  type FilterFieldDef,
} from './FilterBar'

const fields: FilterFieldDef[] = [
  { name: 'nome', label: 'Nome', type: 'string' },
  {
    name: 'tipo',
    label: 'Tipo',
    type: 'enum',
    options: [
      { value: 'RECEITA', label: 'Receita' },
      { value: 'DESPESA', label: 'Despesa' },
    ],
  },
]

const activeFilters: ActiveFilter[] = [
  { field: 'tipo', label: 'Tipo', value: 'RECEITA', displayValue: 'Receita' },
]

function noop() {}

describe('FilterBar', () => {
  it('renderiza o botao de adicionar filtro', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={[]}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByRole('button', { name: /filtro/i })).toBeInTheDocument()
  })

  it('renderiza um chip por filtro ativo com label e displayValue', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={activeFilters}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByText(/Tipo: Receita/)).toBeInTheDocument()
  })

  it('nao exibe "Limpar tudo" quando nao ha filtros ativos', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={[]}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.queryByRole('button', { name: /limpar tudo/i })).toBeNull()
  })

  it('exibe "Limpar tudo" quando ha pelo menos um filtro ativo', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={activeFilters}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(
      screen.getByRole('button', { name: /limpar tudo/i }),
    ).toBeInTheDocument()
  })

  it('chama onRemove ao clicar no X de um chip', async () => {
    const onRemove = vi.fn()
    render(
      <FilterBar
        fields={fields}
        activeFilters={activeFilters}
        onAdd={noop}
        onRemove={onRemove}
        onClear={noop}
      />,
    )

    await userEvent.click(
      screen.getByRole('button', { name: /remover filtro tipo/i }),
    )

    expect(onRemove).toHaveBeenCalledWith('tipo')
  })

  it('chama onClear ao clicar em "Limpar tudo"', async () => {
    const onClear = vi.fn()
    render(
      <FilterBar
        fields={fields}
        activeFilters={activeFilters}
        onAdd={noop}
        onRemove={noop}
        onClear={onClear}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: /limpar tudo/i }))

    expect(onClear).toHaveBeenCalledOnce()
  })

  it('renderiza multiplos chips quando ha varios filtros ativos', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={[
          ...activeFilters,
          { field: 'nome', label: 'Nome', value: 'abc', displayValue: 'abc' },
        ]}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByText(/Tipo: Receita/)).toBeInTheDocument()
    expect(screen.getByText(/Nome: abc/)).toBeInTheDocument()
  })
})
