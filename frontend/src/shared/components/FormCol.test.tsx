import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { FormCol } from './FormCol'

describe('FormCol', () => {
  it('renderiza os filhos', () => {
    render(
      <FormCol>
        <span>conteudo</span>
      </FormCol>,
    )
    expect(screen.getByText('conteudo')).toBeInTheDocument()
  })

  it('aplica col-span-12 por padrao quando span nao e passado', () => {
    const { container } = render(
      <FormCol>
        <span>x</span>
      </FormCol>,
    )
    const col = container.firstElementChild as HTMLElement
    expect(col).toHaveClass('col-span-12')
  })

  it('aplica a classe de span correspondente ao valor passado', () => {
    const { container } = render(
      <FormCol span={6}>
        <span>x</span>
      </FormCol>,
    )
    const col = container.firstElementChild as HTMLElement
    expect(col).toHaveClass('col-span-6')
  })

  it('usa strings literais completas para cada span de 1 a 12', () => {
    for (let span = 1; span <= 12; span++) {
      const { container } = render(
        <FormCol span={span as 1}>
          <span>x</span>
        </FormCol>,
      )
      const col = container.firstElementChild as HTMLElement
      expect(col).toHaveClass(`col-span-${span}`)
    }
  })

  it('mescla className adicional', () => {
    const { container } = render(
      <FormCol span={4} className="px-2">
        <span>x</span>
      </FormCol>,
    )
    const col = container.firstElementChild as HTMLElement
    expect(col).toHaveClass('col-span-4', 'px-2')
  })
})
