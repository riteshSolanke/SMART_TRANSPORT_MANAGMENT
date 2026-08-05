import { beforeEach, describe, expect, it, vi } from 'vitest'

const client = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('./apiClient.js', () => ({
  apiClient: client,
  unwrap: (response) => response.data.data,
}))

import { ticketsApi } from './api.js'

describe('ticketsApi contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('requests availability for the selected service', async () => {
    const params = { routeId: 1, scheduleId: 2, serviceDate: '2026-08-12' }
    client.get.mockResolvedValueOnce({
      data: { data: { capacity: 40, remainingSeats: 33 } },
    })

    await expect(ticketsApi.availability(params)).resolves.toEqual({
      capacity: 40,
      remainingSeats: 33,
    })
    expect(client.get).toHaveBeenCalledWith('/api/tickets/availability', { params })
  })

  it('sends the supplied booking idempotency key', async () => {
    const payload = { routeId: 1, scheduleId: 2 }
    client.post.mockResolvedValueOnce({ data: { data: { ticketId: 9 } } })

    await ticketsApi.book(payload, 'booking-stable-key')

    expect(client.post).toHaveBeenCalledWith('/api/tickets', payload, {
      headers: { 'Idempotency-Key': 'booking-stable-key' },
    })
  })
})
