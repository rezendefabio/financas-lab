import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useCurrentUser } from './use-current-user'
import * as authLib from '@/shared/lib/auth'

vi.mock('@/shared/lib/auth', () => ({
  getCurrentUserEmail: vi.fn(),
}))

const mockGetEmail = vi.mocked(authLib.getCurrentUserEmail)

describe('useCurrentUser', () => {
  beforeEach(() => vi.clearAllMocks())

  it('retorna email e inicial quando logado', () => {
    mockGetEmail.mockReturnValue('fabio@test.com')
    const { result } = renderHook(() => useCurrentUser())
    expect(result.current.email).toBe('fabio@test.com')
    expect(result.current.initials).toBe('F')
  })

  it('retorna null e "?" quando nao logado', () => {
    mockGetEmail.mockReturnValue(null)
    const { result } = renderHook(() => useCurrentUser())
    expect(result.current.email).toBeNull()
    expect(result.current.initials).toBe('?')
  })
})
