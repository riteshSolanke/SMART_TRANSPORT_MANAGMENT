import { format, isValid, parseISO } from 'date-fns'

export function formatCurrency(value) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))
}

export function formatNumber(value) {
  return new Intl.NumberFormat('en-IN').format(Number(value || 0))
}

export function formatDate(value, pattern = 'dd MMM yyyy') {
  if (!value) return '—'
  const parsed = typeof value === 'string' ? parseISO(value) : value
  return isValid(parsed) ? format(parsed, pattern) : '—'
}

export function formatTime(value) {
  if (!value) return '—'
  const [hours = '00', minutes = '00'] = String(value).split(':')
  const date = new Date()
  date.setHours(Number(hours), Number(minutes), 0, 0)
  return format(date, 'hh:mm a')
}

export function humanize(value) {
  if (!value) return 'Unknown'
  return String(value)
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function createIdempotencyKey(prefix) {
  if (globalThis.crypto?.randomUUID) {
    return `${prefix}-${globalThis.crypto.randomUUID()}`
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function createIdempotencyAttempt(prefix) {
  let currentAttempt = null

  return {
    keyFor(payload) {
      const fingerprint = JSON.stringify(payload)
      if (!currentAttempt || currentAttempt.fingerprint !== fingerprint) {
        currentAttempt = {
          fingerprint,
          key: createIdempotencyKey(prefix),
        }
      }
      return currentAttempt.key
    },
    reset() {
      currentAttempt = null
    },
  }
}

