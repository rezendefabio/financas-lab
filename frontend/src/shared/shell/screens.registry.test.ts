import { describe, it, expect } from 'vitest'
import {
  screens,
  getAllScreens,
  findScreenByCode,
  findScreenByPath,
  SCREEN_CODE_REGEX,
  MAX_MENU_DEPTH,
} from './screens.registry'

describe('screens.registry', () => {
  it('getAllScreens retorna o manifesto completo', () => {
    expect(getAllScreens()).toBe(screens)
    expect(getAllScreens().length).toBe(19)
  })

  it('todos os codes seguem o formato MOD-ENT-NNN', () => {
    for (const screen of screens) {
      expect(screen.code, `${screen.code} deveria casar com o regex`).toMatch(
        SCREEN_CODE_REGEX,
      )
    }
  })

  it('todos os codes sao unicos', () => {
    const codes = screens.map((s) => s.code)
    expect(new Set(codes).size).toBe(codes.length)
  })

  it('todos os menuPath respeitam a profundidade maxima', () => {
    for (const screen of screens) {
      expect(screen.menuPath.length).toBeGreaterThan(0)
      expect(screen.menuPath.length).toBeLessThanOrEqual(MAX_MENU_DEPTH)
    }
  })

  it('todas as telas tem permissions vazio nesta fase', () => {
    for (const screen of screens) {
      expect(screen.permissions).toEqual([])
    }
  })

  describe('findScreenByCode', () => {
    it('localiza uma tela existente pelo code', () => {
      const conta = findScreenByCode('FIN-CTA-001')
      expect(conta?.title).toBe('Contas')
      expect(conta?.path).toBe('/contas')
    })

    it('retorna undefined para code inexistente', () => {
      expect(findScreenByCode('XXX-YYY-999')).toBeUndefined()
    })
  })

  describe('findScreenByPath', () => {
    it('localiza a tela pela rota exata', () => {
      expect(findScreenByPath('/contas')?.code).toBe('FIN-CTA-001')
    })

    it('resolve a raiz somente de forma exata', () => {
      expect(findScreenByPath('/')?.code).toBe('REL-DSH-001')
    })

    it('resolve rota filha para a tela base', () => {
      expect(findScreenByPath('/contas/novo')?.code).toBe('FIN-CTA-001')
      expect(findScreenByPath('/contas/abc-123')?.code).toBe('FIN-CTA-001')
    })

    it('retorna undefined para rota nao registrada', () => {
      expect(findScreenByPath('/inexistente')).toBeUndefined()
    })
  })
})
