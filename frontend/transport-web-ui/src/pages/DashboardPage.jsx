import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  FiArrowRight,
  FiBarChart2,
  FiCalendar,
  FiCheckCircle,
  FiClock,
  FiCreditCard,
  FiMap,
  FiSearch,
  FiTag,
  FiTruck,
} from 'react-icons/fi'
import { analyticsApi, routesApi, ticketsApi, vehiclesApi } from '../lib/api.js'
import { useAuth } from '../context/authContext.js'
import { formatCurrency, formatDate, formatTime, humanize } from '../lib/formatters.js'
import ErrorState from '../components/ui/ErrorState.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import StatCard from '../components/ui/StatCard.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

const fleetRoles = ['CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN']
const managerRoles = ['TRANSPORT_MANAGER', 'ADMIN']

export default function DashboardPage() {
  const { user, role } = useAuth()
  const today = new Date().toISOString().slice(0, 10)
  const monthStart = `${today.slice(0, 8)}01`
  const isPassenger = role === 'PASSENGER'
  const canSeeFleet = fleetRoles.includes(role)

  const routesQuery = useQuery({
    queryKey: ['routes'],
    queryFn: routesApi.all,
  })
  const ticketsQuery = useQuery({
    queryKey: ['tickets', 'mine'],
    queryFn: ticketsApi.mine,
    enabled: ['PASSENGER', 'TRANSPORT_MANAGER', 'ADMIN'].includes(role),
  })
  const vehiclesQuery = useQuery({
    queryKey: ['vehicles'],
    queryFn: vehiclesApi.all,
    enabled: canSeeFleet,
  })
  const revenueQuery = useQuery({
    queryKey: ['analytics', 'revenue', monthStart, today],
    queryFn: () => analyticsApi.revenue({ from: monthStart, to: today }),
    enabled: managerRoles.includes(role),
  })

  const routes = routesQuery.data || []
  const tickets = ticketsQuery.data || []
  const vehicles = vehiclesQuery.data || []
  const upcomingTickets = tickets
    .filter((ticket) => ['BOOKED', 'CONFIRMED'].includes(ticket.status))
    .sort((a, b) => String(a.serviceDate).localeCompare(String(b.serviceDate)))
  const activeVehicles = vehicles.filter((vehicle) => vehicle.status === 'IN_SERVICE')
  const pendingTickets = tickets.filter((ticket) => ticket.status === 'PENDING_PAYMENT')

  const firstLoad =
    routesQuery.isLoading ||
    (isPassenger && ticketsQuery.isLoading) ||
    (canSeeFleet && vehiclesQuery.isLoading)

  if (firstLoad) return <PageLoader label="Building today’s transport overview" />

  return (
    <div className="page-stack">
      <section className="dashboard-hero">
        <div className="dashboard-hero__content">
          <span className="eyebrow eyebrow--light">Good day, {user?.name?.split(' ')[0] || 'traveller'}</span>
          <h2>{heroTitle(role)}</h2>
          <p>{heroDescription(role)}</p>
          <div className="dashboard-hero__actions">
            <Link className="button button--light" to={role === 'PASSENGER' ? '/routes' : primaryPath(role)}>
              {role === 'PASSENGER' ? <FiSearch /> : <FiArrowRight />}
              {role === 'PASSENGER' ? 'Find a route' : primaryLabel(role)}
            </Link>
            <Link className="button button--glass" to="/profile">
              View profile
            </Link>
          </div>
        </div>
        <div className="dashboard-hero__visual" aria-hidden="true">
          <span className="orbit orbit--one" />
          <span className="orbit orbit--two" />
          <div className="route-map">
            <span className="route-map__stop route-map__stop--one" />
            <span className="route-map__stop route-map__stop--two" />
            <span className="route-map__stop route-map__stop--three" />
            <span className="route-map__line" />
            <span className="route-map__vehicle">
              <FiTruck />
            </span>
          </div>
          <div className="dashboard-hero__metric">
            <FiCheckCircle />
            <div>
              <strong>Gateway secured</strong>
              <span>Role: {humanize(role)}</span>
            </div>
          </div>
        </div>
      </section>

      {routesQuery.isError ? (
        <ErrorState error={routesQuery.error} onRetry={routesQuery.refetch} compact />
      ) : (
        <section className="stat-grid">
          <StatCard
            label="Active routes"
            value={routes.filter((route) => route.active).length}
            helper={`${routes.length} network records`}
            icon={FiMap}
            tone="blue"
          />
          {role === 'PASSENGER' ? (
            <>
              <StatCard
                label="Upcoming journeys"
                value={upcomingTickets.length}
                helper="Ready for travel"
                icon={FiTag}
                tone="teal"
              />
              <StatCard
                label="Awaiting payment"
                value={pendingTickets.length}
                helper="Complete before expiry"
                icon={FiCreditCard}
                tone="amber"
              />
            </>
          ) : fleetRoles.includes(role) ? (
            <>
              <StatCard
                label="Fleet vehicles"
                value={vehicles.length}
                helper={`${activeVehicles.length} currently in service`}
                icon={FiTruck}
                tone="teal"
              />
              <StatCard
                label={managerRoles.includes(role) ? 'Net revenue' : 'Service ready'}
                value={
                  managerRoles.includes(role)
                    ? formatCurrency(revenueQuery.data?.netRevenue)
                    : vehicles.filter((vehicle) => vehicle.status === 'AVAILABLE').length
                }
                helper={managerRoles.includes(role) ? 'Current month' : 'Available vehicles'}
                icon={managerRoles.includes(role) ? FiBarChart2 : FiCheckCircle}
                tone="amber"
              />
            </>
          ) : (
            <>
              <StatCard label="Service date" value={formatDate(today, 'dd MMM')} icon={FiCalendar} />
              <StatCard label="Workspace" value="Ready" icon={FiCheckCircle} tone="teal" />
            </>
          )}
        </section>
      )}

      <section className="dashboard-grid">
        <article className="panel panel--wide">
          <div className="panel__header">
            <div>
              <span className="eyebrow">{role === 'PASSENGER' ? 'Your journeys' : 'Network snapshot'}</span>
              <h3>{role === 'PASSENGER' ? 'Upcoming tickets' : 'Active routes'}</h3>
            </div>
            <Link to={role === 'PASSENGER' ? '/tickets' : '/routes'} className="text-link">
              View all <FiArrowRight />
            </Link>
          </div>

          {role === 'PASSENGER' ? (
            upcomingTickets.length ? (
              <div className="journey-list">
                {upcomingTickets.slice(0, 3).map((ticket) => (
                  <div className="journey-row" key={ticket.ticketId}>
                    <span className="journey-row__date">
                      <strong>{formatDate(ticket.serviceDate, 'dd')}</strong>
                      <small>{formatDate(ticket.serviceDate, 'MMM')}</small>
                    </span>
                    <div className="journey-row__main">
                      <strong>Route #{ticket.routeId}</strong>
                      <span>
                        <FiClock /> {formatTime(ticket.departureTime)} · {ticket.passengerCount} passenger
                      </span>
                    </div>
                    <StatusBadge status={ticket.status} />
                    <span className="journey-row__fare">{formatCurrency(ticket.fareAmount)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="inline-empty">
                <FiTag />
                <div>
                  <strong>No upcoming journey</strong>
                  <span>Search the network and book your next ticket.</span>
                </div>
                <Link to="/routes" className="button button--secondary button--small">
                  Browse routes
                </Link>
              </div>
            )
          ) : (
            <div className="route-snapshot-list">
              {routes.slice(0, 5).map((route) => (
                <div className="route-snapshot" key={route.routeId}>
                  <span className="route-code">R{String(route.routeId).padStart(2, '0')}</span>
                  <div>
                    <strong>{route.routeName}</strong>
                    <span>{route.startPoint} → {route.endPoint}</span>
                  </div>
                  <span>{route.schedules?.length || 0} schedules</span>
                  <StatusBadge active={route.active} />
                </div>
              ))}
            </div>
          )}
        </article>

        <aside className="panel quick-panel">
          <div className="panel__header">
            <div>
              <span className="eyebrow">Shortcuts</span>
              <h3>Quick actions</h3>
            </div>
          </div>
          <div className="quick-actions">
            {quickActions(role).map(({ label, detail, path, icon: Icon }) => (
              <Link to={path} key={label}>
                <span><Icon /></span>
                <div>
                  <strong>{label}</strong>
                  <small>{detail}</small>
                </div>
                <FiArrowRight />
              </Link>
            ))}
          </div>
        </aside>
      </section>
    </div>
  )
}

function heroTitle(role) {
  if (role === 'PASSENGER') return 'Where would you like to go today?'
  if (role === 'CONDUCTOR') return 'Your service workspace is ready.'
  if (role === 'DISPATCHER') return 'Keep every vehicle moving with confidence.'
  if (role === 'TRANSPORT_MANAGER') return 'Turn network activity into better service.'
  return 'The entire transport operation, in one view.'
}

function heroDescription(role) {
  if (role === 'PASSENGER') return 'Search routes, compare schedules and complete your journey in a few simple steps.'
  if (role === 'CONDUCTOR') return 'Review routes, vehicle status and the information you need for today’s service.'
  if (role === 'DISPATCHER') return 'Monitor fleet readiness and coordinate assignments across the network.'
  if (role === 'TRANSPORT_MANAGER') return 'Track operations, revenue and punctuality with trusted service data.'
  return 'Manage people, routes, fleet operations and performance with secure role-based control.'
}

function primaryPath(role) {
  if (role === 'CONDUCTOR' || role === 'DISPATCHER') return '/fleet'
  if (role === 'TRANSPORT_MANAGER') return '/analytics'
  return '/users'
}

function primaryLabel(role) {
  if (role === 'CONDUCTOR' || role === 'DISPATCHER') return 'Open fleet operations'
  if (role === 'TRANSPORT_MANAGER') return 'Review analytics'
  return 'Manage users'
}

function quickActions(role) {
  if (role === 'PASSENGER') {
    return [
      { label: 'Find a route', detail: 'Search stops and schedules', path: '/routes', icon: FiSearch },
      { label: 'My tickets', detail: 'Upcoming and previous trips', path: '/tickets', icon: FiTag },
      { label: 'Payment history', detail: 'Review fare transactions', path: '/payments', icon: FiCreditCard },
    ]
  }
  if (role === 'CONDUCTOR' || role === 'DISPATCHER') {
    return [
      { label: 'Fleet status', detail: 'Review service readiness', path: '/fleet', icon: FiTruck },
      { label: 'Route network', detail: 'Stops and schedules', path: '/routes', icon: FiMap },
      { label: 'Account', detail: 'Profile and preferences', path: '/profile', icon: FiCheckCircle },
    ]
  }
  return [
    { label: 'Analytics', detail: 'Usage, revenue and punctuality', path: '/analytics', icon: FiBarChart2 },
    { label: 'Fleet operations', detail: 'Vehicles and assignments', path: '/fleet', icon: FiTruck },
    { label: 'Route management', detail: 'Network and schedules', path: '/routes', icon: FiMap },
  ]
}
