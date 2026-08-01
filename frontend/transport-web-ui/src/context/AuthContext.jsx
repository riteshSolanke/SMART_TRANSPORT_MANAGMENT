import { useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '../lib/api.js'
import { clearSession, getSession, setSession, subscribeToSession } from '../lib/session.js'
import { AuthContext } from './authContext.js'

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(() => getSession())
  const [isBootstrapping, setIsBootstrapping] = useState(Boolean(getSession()))

  useEffect(() => subscribeToSession(setSessionState), [])

  useEffect(() => {
    let active = true

    async function restoreUser() {
      if (!session?.accessToken) {
        if (active) setIsBootstrapping(false)
        return
      }

      try {
        const user = await authApi.me()
        if (active) {
          const nextSession = { ...getSession(), user }
          setSession(nextSession)
          setSessionState(nextSession)
        }
      } catch {
        if (active) clearSession()
      } finally {
        if (active) setIsBootstrapping(false)
      }
    }

    restoreUser()
    return () => {
      active = false
    }
    // Session restoration should run once on application start.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const establishSession = useCallback((authResponse) => {
    setSession(authResponse)
    setSessionState(authResponse)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // The local session must still be cleared if the server is unavailable.
    } finally {
      clearSession()
      setSessionState(null)
    }
  }, [])

  const updateCurrentUser = useCallback((user) => {
    const nextSession = { ...getSession(), user }
    setSession(nextSession)
    setSessionState(nextSession)
  }, [])

  const value = useMemo(
    () => ({
      session,
      user: session?.user || null,
      role: session?.user?.role || null,
      isAuthenticated: Boolean(session?.accessToken),
      isBootstrapping,
      establishSession,
      updateCurrentUser,
      logout,
    }),
    [session, isBootstrapping, establishSession, updateCurrentUser, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
