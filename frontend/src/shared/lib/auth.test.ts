import { describe, it, expect, beforeEach } from 'vitest'
import {
  getToken,
  setToken,
  clearToken,
  isAuthenticated,
  parseJwtPayload,
  getCurrentUserEmail,
} from './auth'

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when no token is stored', () => {
    expect(getToken()).toBeNull()
  })

  it('stores and retrieves a token', () => {
    setToken('my-token')
    expect(getToken()).toBe('my-token')
  })

  it('clears the stored token', () => {
    setToken('my-token')
    clearToken()
    expect(getToken()).toBeNull()
  })

  it('isAuthenticated returns false when no token', () => {
    expect(isAuthenticated()).toBe(false)
  })

  it('isAuthenticated returns true when token is set', () => {
    setToken('my-token')
    expect(isAuthenticated()).toBe(true)
  })
})

describe('parseJwtPayload', () => {
  it('extrai sub do payload JWT', () => {
    const payload = btoa(JSON.stringify({ sub: 'fabio@test.com' }))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
    const token = `header.${payload}.sig`
    expect(parseJwtPayload(token)).toEqual({ sub: 'fabio@test.com' })
  })

  it('retorna {} para token invalido', () => {
    expect(parseJwtPayload('invalido')).toEqual({})
  })
})

describe('getCurrentUserEmail', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('retorna email do token no localStorage', () => {
    const payload = btoa(JSON.stringify({ sub: 'fabio@test.com' }))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
    setToken(`h.${payload}.s`)
    expect(getCurrentUserEmail()).toBe('fabio@test.com')
    clearToken()
  })

  it('retorna null quando nao ha token', () => {
    clearToken()
    expect(getCurrentUserEmail()).toBeNull()
  })
})
