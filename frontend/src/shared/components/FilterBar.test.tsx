import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import {
  FilterBar,
  OPERATORS_BY_TYPE,
  type ActiveFilter,
  type FilterFieldDef,
} from './FilterBar'

const fields: FilterFieldDef[] = [
  { name: 'nome', label: 'Nome', type: 'string' },
  { name: 'valor', label: 'Valor', type: 'number' },
  { name: 'ativa', label: 'Ativa', type: 'boolean' },
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
  {
    field: 'tipo',
    operator: 'eq',
    operatorLabel: 'igual a',
    label: 'Tipo',
    value: 'RECEITA',
    displayValue: 'Receita',
  },
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

  it('renderiza um chip por filtro ativo com label, operador e displayValue', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={activeFilters}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByText(/Tipo igual a/)).toBeInTheDocument()
    expect(screen.getByText(/Receita/)).toBeInTheDocument()
  })

  it('renderiza chip de filtro boolean sem valor (operador e o valor)', () => {
    render(
      <FilterBar
        fields={fields}
        activeFilters={[
          {
            field: 'ativa',
            operator: 'true',
            operatorLabel: 'verdadeiro',
            label: 'Ativa',
            value: '',
            displayValue: 'verdadeiro',
          },
        ]}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByText(/Ativa verdadeiro/)).toBeInTheDocument()
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
          {
            field: 'nome',
            operator: 'contains',
            operatorLabel: 'contem',
            label: 'Nome',
            value: 'abc',
            displayValue: 'abc',
          },
        ]}
        onAdd={noop}
        onRemove={noop}
        onClear={noop}
      />,
    )

    expect(screen.getByText(/Tipo igual a/)).toBeInTheDocument()
    expect(screen.getByText(/Nome contem/)).toBeInTheDocument()
  })

  describe('OPERATORS_BY_TYPE', () => {
    it('expoe operadores de texto para campos string', () => {
      expect(OPERATORS_BY_TYPE.string.map((o) => o.value)).toEqual([
        'contains',
        'not_contains',
        'eq',
        'neq',
      ])
    })

    it('expoe operadores de comparacao para number e date', () => {
      const esperado = ['eq', 'neq', 'gt', 'gte', 'lt', 'lte']
      expect(OPERATORS_BY_TYPE.number.map((o) => o.value)).toEqual(esperado)
      expect(OPERATORS_BY_TYPE.date.map((o) => o.value)).toEqual(esperado)
    })

    it('expoe verdadeiro/falso como operadores para boolean', () => {
      expect(OPERATORS_BY_TYPE.boolean.map((o) => o.value)).toEqual([
        'true',
        'false',
      ])
    })

    it('expoe igual/diferente para enum', () => {
      expect(OPERATORS_BY_TYPE.enum.map((o) => o.value)).toEqual(['eq', 'neq'])
    })
  })

  describe('popover de tres passos', () => {
    it('abre o popover mostrando o passo 1 (Campo) com Aplicar desabilitado', async () => {
      render(
        <FilterBar
          fields={fields}
          activeFilters={[]}
          onAdd={noop}
          onRemove={noop}
          onClear={noop}
        />,
      )

      await userEvent.click(screen.getByRole('button', { name: /^filtro/i }))

      // Passo 1 visivel: rotulo "Campo" e placeholder do seletor.
      expect(screen.getByText('Campo')).toBeInTheDocument()
      expect(screen.getByText('Selecione o campo')).toBeInTheDocument()
      // Passos 2 e 3 ainda nao renderizados (nenhum campo escolhido).
      expect(screen.queryByText('Operador')).toBeNull()
      expect(screen.queryByText('Valor')).toBeNull()
      // Aplicar comeca desabilitado.
      expect(screen.getByRole('button', { name: /aplicar/i })).toBeDisabled()
    })
  })
})
