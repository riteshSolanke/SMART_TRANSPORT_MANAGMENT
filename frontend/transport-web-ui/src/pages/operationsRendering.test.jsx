import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  routesApi: {
    allDetailed: vi.fn(),
    search: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
  vehiclesApi: {
    all: vi.fn(),
    assignments: vi.fn(),
    create: vi.fn(),
    updateStatus: vi.fn(),
    assign: vi.fn(),
    recordLocation: vi.fn(),
    completeAssignment: vi.fn(),
    cancelAssignment: vi.fn(),
    remove: vi.fn(),
  },
}))

vi.mock('../lib/api.js', () => api)
vi.mock('../context/authContext.js', () => ({
  useAuth: () => ({ role: 'ADMIN' }),
}))
vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}))

import FleetPage from './FleetPage.jsx'
import RoutesPage from './RoutesPage.jsx'

const detailedRoute = {
  routeId: 1,
  routeName: 'Jalana-Solapur',
  startPoint: 'Jalana',
  endPoint: 'Solapur',
  active: true,
  stops: [{ stopId: 1, stopName: 'Jalana' }],
  schedules: [
    {
      scheduleId: 1,
      departureTime: '06:20:00',
      arrivalTime: '11:30:00',
      active: true,
    },
  ],
}

function renderPage(page) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{page}</MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('operations schedule rendering', () => {
  afterEach(() => cleanup())

  beforeEach(() => {
    vi.clearAllMocks()
    api.routesApi.allDetailed.mockResolvedValue([detailedRoute])
    api.routesApi.search.mockResolvedValue([])
    api.vehiclesApi.all.mockResolvedValue([
      {
        vehicleId: 1,
        vehicleNumber: 'MH-12-4345',
        capacity: 40,
        status: 'AVAILABLE',
        active: true,
      },
    ])
    api.vehiclesApi.assignments.mockResolvedValue([
      {
        assignmentId: 7,
        vehicleId: 1,
        routeId: 1,
        scheduleId: 1,
        serviceDate: '2026-08-05',
        status: 'ACTIVE',
        assignedAt: '2026-08-04T09:15:00',
      },
    ])
  })

  it('shows hydrated schedules on the Routes page', async () => {
    renderPage(<RoutesPage />)

    expect(await screen.findByText('Jalana-Solapur')).toBeInTheDocument()
    expect(screen.getByText('1 schedule')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Show route details' }))

    expect(await screen.findByText('06:20 AM')).toBeInTheDocument()
    expect(screen.getByText('to 11:30 AM')).toBeInTheDocument()
  })

  it('shows the departure and arrival time in vehicle assignment history', async () => {
    renderPage(<FleetPage />)

    expect(await screen.findByText('MH-12-4345')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'History' }))

    const historyDialog = await screen.findByRole('dialog', {
      name: 'MH-12-4345 assignments',
    })
    expect(await within(historyDialog).findByText(/Jalana-Solapur/)).toBeInTheDocument()
    expect(within(historyDialog).getByText(/06:20 AM – 11:30 AM/)).toBeInTheDocument()
  })
})
