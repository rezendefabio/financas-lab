import { renderHook, act, waitFor } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useForm } from 'react-hook-form'
import { useDraftForm } from './useDraftForm'
import { useDraftFormsStore } from '@/shared/shell/draft-forms-store'

vi.mock('next/navigation', () => ({
  usePathname: () => '/categorias/novo',
}))

interface Values {
  nome: string
  tipo: string
}

describe('useDraftForm', () => {
  beforeEach(() => {
    useDraftFormsStore.setState({ drafts: {} })
  })

  it('restaura o rascunho salvo ao montar', async () => {
    useDraftFormsStore.getState().save('/categorias/novo', {
      nome: 'Restaurado',
      tipo: 'RECEITA',
    })

    const { result } = renderHook(() => {
      const form = useForm<Values>({
        defaultValues: { nome: '', tipo: 'DESPESA' },
      })
      useDraftForm(form)
      return form
    })

    await waitFor(() => {
      expect(result.current.getValues('nome')).toBe('Restaurado')
      expect(result.current.getValues('tipo')).toBe('RECEITA')
    })
  })

  it('expoe clearDraft e resetWithDraft como funcoes', () => {
    const { result } = renderHook(() => {
      const form = useForm<Values>({
        defaultValues: { nome: '', tipo: 'DESPESA' },
      })
      return useDraftForm(form)
    })

    expect(typeof result.current.clearDraft).toBe('function')
    expect(typeof result.current.resetWithDraft).toBe('function')
  })

  it('clearDraft remove o rascunho do store', () => {
    useDraftFormsStore.getState().save('/categorias/novo', { nome: 'X', tipo: 'DESPESA' })

    const { result } = renderHook(() => {
      const form = useForm<Values>({ defaultValues: { nome: '', tipo: 'DESPESA' } })
      const draft = useDraftForm(form)
      return draft
    })

    act(() => {
      result.current.clearDraft()
    })

    expect(useDraftFormsStore.getState().getDraft('/categorias/novo')).toBeNull()
  })

  it('resetWithDraft usa rascunho quando existe', () => {
    useDraftFormsStore.getState().save('/categorias/novo', {
      nome: 'Do rascunho',
      tipo: 'RECEITA',
    })

    const { result } = renderHook(() => {
      const form = useForm<Values>({ defaultValues: { nome: '', tipo: 'DESPESA' } })
      const draft = useDraftForm(form)
      return { form, draft }
    })

    act(() => {
      result.current.draft.resetWithDraft({ nome: 'Base', tipo: 'DESPESA' })
    })

    expect(result.current.form.getValues('nome')).toBe('Do rascunho')
  })

  it('resetWithDraft usa base quando nao ha rascunho', () => {
    const { result } = renderHook(() => {
      const form = useForm<Values>({ defaultValues: { nome: '', tipo: 'DESPESA' } })
      const draft = useDraftForm(form)
      return { form, draft }
    })

    act(() => {
      result.current.draft.resetWithDraft({ nome: 'Base', tipo: 'RECEITA' })
    })

    expect(result.current.form.getValues('nome')).toBe('Base')
    expect(result.current.form.getValues('tipo')).toBe('RECEITA')
  })

  it('salva rascunho imediatamente ao desmontar se form estiver sujo', () => {
    vi.useFakeTimers()

    const { result, unmount } = renderHook(() => {
      const form = useForm<Values>({ defaultValues: { nome: '', tipo: 'DESPESA' } })
      useDraftForm(form)
      return form
    })

    act(() => {
      result.current.setValue('nome', 'Nao salvo ainda', { shouldDirty: true })
    })

    // Desmonta antes dos 400ms dispararem
    unmount()
    vi.useRealTimers()

    expect(useDraftFormsStore.getState().getDraft('/categorias/novo')).toMatchObject({
      nome: 'Nao salvo ainda',
    })
  })

  it('resetWithDraft mantem referencia estavel entre renders', () => {
    const { result, rerender } = renderHook(() => {
      const form = useForm<Values>({ defaultValues: { nome: '', tipo: 'DESPESA' } })
      return useDraftForm(form)
    })

    const firstRef = result.current.resetWithDraft
    rerender()
    expect(result.current.resetWithDraft).toBe(firstRef)
  })
})
