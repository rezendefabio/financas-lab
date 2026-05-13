import { describe, it, expect, beforeEach } from 'vitest'
import { getToken, setToken, clearToken, isAuthenticated } from './auth'

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
