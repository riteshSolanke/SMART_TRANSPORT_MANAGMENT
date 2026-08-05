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

import { routesApi } from './api.js'

describe('routesApi detailed route loading', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('hydrates route summaries with their stops and schedules', async () => {
    client.get
      .mockResolvedValueOnce({
        data: {
          data: [
            { routeId: 1, routeName: 'North Line', schedules: [] },
            { routeId: 2, routeName: 'South Line', schedules: [] },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: {
            routeId: 1,
            routeName: 'North Line',
            schedules: [{ scheduleId: 11, departureTime: '06:20:00' }],
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          data: {
            routeId: 2,
            routeName: 'South Line',
            schedules: [{ scheduleId: 22, departureTime: '08:30:00' }],
          },
        },
      })

    const routes = await routesApi.allDetailed()

    expect(client.get).toHaveBeenNthCalledWith(1, '/api/routes')
    expect(client.get).toHaveBeenNthCalledWith(2, '/api/routes/1')
    expect(client.get).toHaveBeenNthCalledWith(3, '/api/routes/2')
    expect(routes).toHaveLength(2)
    expect(routes[0].schedules[0].departureTime).toBe('06:20:00')
    expect(routes[1].schedules[0].departureTime).toBe('08:30:00')
  })

  it('does not issue detail calls when no routes exist', async () => {
    client.get.mockResolvedValueOnce({ data: { data: [] } })

    await expect(routesApi.allDetailed()).resolves.toEqual([])
    expect(client.get).toHaveBeenCalledTimes(1)
  })
})
