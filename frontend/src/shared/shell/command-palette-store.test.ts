import { describe, it, expect, beforeEach } from 'vitest'
import { useCommandPaletteStore } from './command-palette-store'

describe('useCommandPaletteStore', () => {
  beforeEach(() => {
    useCommandPaletteStore.setState({ open: false })
  })

  it('inicia fechado', () => {
    expect(useCommandPaletteStore.getState().open).toBe(false)
  })

  it('setOpen(true) abre o palette', () => {
    useCommandPaletteStore.getState().setOpen(true)
    expect(useCommandPaletteStore.getState().open).toBe(true)
  })

  it('setOpen(false) fecha o palette', () => {
    useCommandPaletteStore.getState().setOpen(true)
    useCommandPaletteStore.getState().setOpen(false)
    expect(useCommandPaletteStore.getState().open).toBe(false)
  })
})
