import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { FormGrid } from './FormGrid'

describe('FormGrid', () => {
  it('renderiza os filhos', () => {
    render(
      <FormGrid>
        <span>conteudo</span>
      </FormGrid>,
    )
    expect(screen.getByText('conteudo')).toBeInTheDocument()
  })

  it('aplica as classes de grid de 12 colunas', () => {
    const { container } = render(
      <FormGrid>
        <span>x</span>
      </FormGrid>,
    )
    const grid = container.firstElementChild as HTMLElement
    expect(grid).toHaveClass('grid', 'grid-cols-12', 'gap-4')
  })

  it('mescla className adicional', () => {
    const { container } = render(
      <FormGrid className="mt-8">
        <span>x</span>
      </FormGrid>,
    )
    const grid = container.firstElementChild as HTMLElement
    expect(grid).toHaveClass('grid-cols-12', 'mt-8')
  })
})
