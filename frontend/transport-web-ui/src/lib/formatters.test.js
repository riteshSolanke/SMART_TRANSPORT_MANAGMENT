import { describe, expect, it } from 'vitest'
import {
  createIdempotencyAttempt,
  createIdempotencyKey,
  formatCurrency,
  formatNumber,
  formatTime,
  humanize,
} from './formatters.js'

describe('formatters', () => {
  it('formats monetary and numeric values for the Indian locale', () => {
    expect(formatCurrency(1250.5)).toContain('1,250.50')
    expect(formatNumber(1234567)).toBe('12,34,567')
  })

  it('turns service statuses into readable labels', () => {
    expect(humanize('PENDING_PAYMENT')).toBe('Pending Payment')
    expect(humanize()).toBe('Unknown')
  })

  it('formats backend time values and handles missing values', () => {
    expect(formatTime('14:30:00')).toBe('02:30 PM')
    expect(formatTime()).toBe('—')
  })

  it('creates keys scoped to the requested operation', () => {
    expect(createIdempotencyKey('booking')).toMatch(/^booking-.+/)
  })

  it('reuses an idempotency key until an attempt succeeds or changes', () => {
    const attempt = createIdempotencyAttempt('booking')
    const payload = { routeId: 1, passengerCount: 2 }
    const firstKey = attempt.keyFor(payload)

    expect(attempt.keyFor({ ...payload })).toBe(firstKey)
    expect(attempt.keyFor({ ...payload, passengerCount: 3 })).not.toBe(firstKey)

    const changedKey = attempt.keyFor(payload)
    attempt.reset()
    expect(attempt.keyFor(payload)).not.toBe(changedKey)
  })
})
