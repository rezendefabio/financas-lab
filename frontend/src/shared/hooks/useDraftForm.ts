'use client'

import { useCallback, useEffect, useRef } from 'react'
import { usePathname } from 'next/navigation'
import type { FieldValues, UseFormReturn } from 'react-hook-form'
import { useDraftFormsStore } from '@/shared/shell/draft-forms-store'

export function useDraftForm<T extends FieldValues>(
  form: UseFormReturn<T>,
): { clearDraft: () => void; resetWithDraft: (base: T) => void } {
  const pathname = usePathname()
  const { save, getDraft, clear } = useDraftFormsStore()
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Restore no mount
  useEffect(() => {
    const draft = getDraft(pathname) as T | null
    if (draft) {
      form.reset(draft)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Auto-save com debounce 400ms
  useEffect(() => {
    const subscription = form.watch((values) => {
      if (!form.formState.isDirty) return
      if (timerRef.current) clearTimeout(timerRef.current)
      timerRef.current = setTimeout(() => {
        save(pathname, values)
      }, 400)
    })
    return () => {
      subscription.unsubscribe()
      if (timerRef.current) clearTimeout(timerRef.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form, pathname, save])

  // resetWithDraft estavel por ref
  const resetWithDraftRef = useRef((base: T) => {
    const draft = useDraftFormsStore.getState().getDraft(pathname) as T | null
    form.reset(draft ?? base)
  })
  useEffect(() => {
    resetWithDraftRef.current = (base: T) => {
      const draft = useDraftFormsStore.getState().getDraft(pathname) as T | null
      form.reset(draft ?? base)
    }
  }, [pathname, form])

  const resetWithDraft = useCallback(
    (base: T) => resetWithDraftRef.current(base),
    [],
  )
  const clearDraft = useCallback(() => clear(pathname), [clear, pathname])

  return {
    clearDraft,
    resetWithDraft,
  }
}
