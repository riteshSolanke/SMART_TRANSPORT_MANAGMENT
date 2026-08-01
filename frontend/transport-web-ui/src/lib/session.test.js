import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearSession,
  getSession,
  setSession,
  subscribeToSession,
} from './session.js'

describe('session storage', () => {
  beforeEach(() => {
    clearSession()
    sessionStorage.clear()
  })

  it('stores and clears the active authentication session', () => {
    const session = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: { userId: 1, role: 'PASSENGER' },
    }

    setSession(session)
    expect(getSession()).toEqual(session)

    clearSession()
    expect(getSession()).toBeNull()
  })

  it('notifies subscribers when the session changes', () => {
    const listener = vi.fn()
    const unsubscribe = subscribeToSession(listener)
    const session = { accessToken: 'token' }

    setSession(session)
    expect(listener).toHaveBeenCalledWith(session)

    unsubscribe()
    clearSession()
    expect(listener).toHaveBeenCalledTimes(1)
  })
})
