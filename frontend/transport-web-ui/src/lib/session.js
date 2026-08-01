const SESSION_KEY = 'transitflow.session'
const SESSION_EVENT = 'transitflow:session-changed'

let memorySession = null

function readStoredSession() {
  try {
    const value = sessionStorage.getItem(SESSION_KEY)
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

export function getSession() {
  if (!memorySession) memorySession = readStoredSession()
  return memorySession
}

export function setSession(session) {
  memorySession = session
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new CustomEvent(SESSION_EVENT, { detail: session }))
}

export function clearSession() {
  memorySession = null
  sessionStorage.removeItem(SESSION_KEY)
  window.dispatchEvent(new CustomEvent(SESSION_EVENT, { detail: null }))
}

export function subscribeToSession(listener) {
  const handler = (event) => listener(event.detail)
  window.addEventListener(SESSION_EVENT, handler)
  return () => window.removeEventListener(SESSION_EVENT, handler)
}
