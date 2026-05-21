import { describe, it, expect, beforeEach } from 'vitest'
import { useDraftFormsStore } from './draft-forms-store'

describe('useDraftFormsStore', () => {
  beforeEach(() => {
    useDraftFormsStore.setState({ drafts: {} })
  })

  it('inicia com drafts vazio', () => {
    expect(useDraftFormsStore.getState().drafts).toEqual({})
  })

  it('save grava valores na chave informada', () => {
    useDraftFormsStore.getState().save('/categorias/novo', { nome: 'Alimentacao' })
    expect(useDraftFormsStore.getState().drafts['/categorias/novo']).toEqual({
      nome: 'Alimentacao',
    })
  })

  it('getDraft retorna o rascunho persistido na chave', () => {
    useDraftFormsStore.getState().save('/tags/novo', { nome: 'Urgente' })
    expect(useDraftFormsStore.getState().getDraft('/tags/novo')).toEqual({
      nome: 'Urgente',
    })
  })

  it('getDraft retorna null quando nao existe rascunho', () => {
    expect(useDraftFormsStore.getState().getDraft('/inexistente')).toBeNull()
  })

  it('clear remove o rascunho da chave', () => {
    useDraftFormsStore.getState().save('/metas/novo', { nome: 'Carro' })
    useDraftFormsStore.getState().clear('/metas/novo')
    expect(useDraftFormsStore.getState().getDraft('/metas/novo')).toBeNull()
  })

  it('clear preserva rascunhos de outras chaves', () => {
    useDraftFormsStore.getState().save('/tags/novo', { nome: 'A' })
    useDraftFormsStore.getState().save('/payees/novo', { nome: 'B' })
    useDraftFormsStore.getState().clear('/tags/novo')
    expect(useDraftFormsStore.getState().getDraft('/tags/novo')).toBeNull()
    expect(useDraftFormsStore.getState().getDraft('/payees/novo')).toEqual({
      nome: 'B',
    })
  })

  it('save sobrescreve o rascunho anterior da mesma chave', () => {
    useDraftFormsStore.getState().save('/tags/novo', { nome: 'A' })
    useDraftFormsStore.getState().save('/tags/novo', { nome: 'B' })
    expect(useDraftFormsStore.getState().getDraft('/tags/novo')).toEqual({
      nome: 'B',
    })
  })
})
