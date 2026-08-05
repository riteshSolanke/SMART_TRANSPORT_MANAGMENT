import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import {
  FiArrowRight,
  FiCalendar,
  FiChevronDown,
  FiChevronUp,
  FiClock,
  FiEdit2,
  FiMapPin,
  FiPlus,
  FiSearch,
  FiTrash2,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import Modal from '../components/ui/Modal.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { useAuth } from '../context/authContext.js'
import { confirmAction } from '../lib/alerts.js'
import { routesApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { formatCurrency, formatTime } from '../lib/formatters.js'

const today = new Date().toISOString().slice(0, 10)

export default function RoutesPage() {
  const { role } = useAuth()
  const queryClient = useQueryClient()
  const canManage = ['TRANSPORT_MANAGER', 'ADMIN'].includes(role)
  const [searchParams, setSearchParams] = useState(null)
  const [expandedRoute, setExpandedRoute] = useState(null)
  const [editingRoute, setEditingRoute] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const searchForm = useForm({
    defaultValues: { from: '', to: '', travelDate: today },
  })
  const routeForm = useForm({
    defaultValues: { routeName: '', startPoint: '', endPoint: '' },
  })

  const routesQuery = useQuery({
    queryKey: ['routes', 'detailed'],
    queryFn: routesApi.allDetailed,
  })
  const searchQuery = useQuery({
    queryKey: ['routes', 'search', searchParams],
    queryFn: () => routesApi.search(searchParams),
    enabled: Boolean(searchParams),
  })

  const saveRoute = useMutation({
    mutationFn: (payload) =>
      editingRoute
        ? routesApi.update(editingRoute.routeId, payload)
        : routesApi.create(payload),
    onSuccess: () => {
      toast.success(editingRoute ? 'Route updated successfully' : 'Route created successfully')
      queryClient.invalidateQueries({ queryKey: ['routes'] })
      closeModal()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const deleteRoute = useMutation({
    mutationFn: routesApi.remove,
    onSuccess: () => {
      toast.success('Route removed from the active network')
      queryClient.invalidateQueries({ queryKey: ['routes'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const routes = useMemo(() => routesQuery.data || [], [routesQuery.data])
  const searchResults = searchQuery.data || []
  const displayedRoutes = searchParams ? searchResults : routes

  const totalSchedules = useMemo(
    () => routes.reduce((total, route) => total + (route.schedules?.length || 0), 0),
    [routes],
  )

  function openCreate() {
    setEditingRoute(null)
    routeForm.reset({ routeName: '', startPoint: '', endPoint: '' })
    setModalOpen(true)
  }

  function openEdit(route) {
    setEditingRoute(route)
    routeForm.reset({
      routeName: route.routeName,
      startPoint: route.startPoint,
      endPoint: route.endPoint,
    })
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingRoute(null)
  }

  async function handleDelete(route) {
    const result = await confirmAction({
      title: `Delete ${route.routeName}?`,
      text: 'The route will no longer be available for new journey searches.',
      confirmText: 'Delete route',
      confirmColor: '#dc2626',
    })
    if (result.isConfirmed) deleteRoute.mutate(route.routeId)
  }

  if (routesQuery.isLoading) return <PageLoader label="Loading routes and schedules" />

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Network planning"
        title="Find the right service"
        description={
          canManage
            ? `${routes.length} routes and ${totalSchedules} schedules are available in the network.`
            : 'Search by origin and destination to compare routes, schedules and estimated fares.'
        }
        actions={
          canManage && (
            <button className="button button--primary" onClick={openCreate}>
              <FiPlus /> Add route
            </button>
          )
        }
      />

      <section className="route-search-card">
        <div className="route-search-card__icon"><FiSearch /></div>
        <form
          onSubmit={searchForm.handleSubmit((values) => {
            if (!values.from.trim() || !values.to.trim()) {
              toast.error('Enter both an origin and destination')
              return
            }
            setSearchParams({
              from: values.from.trim(),
              to: values.to.trim(),
              travelDate: values.travelDate,
            })
          })}
        >
          <label className="field field--search">
            <span>From</span>
            <div className="field__control">
              <FiMapPin />
              <input placeholder="Starting point or stop" {...searchForm.register('from')} />
            </div>
          </label>
          <span className="route-search-card__connector" aria-hidden="true">
            <span />
            <FiArrowRight />
          </span>
          <label className="field field--search">
            <span>To</span>
            <div className="field__control">
              <FiMapPin />
              <input placeholder="Destination stop" {...searchForm.register('to')} />
            </div>
          </label>
          <label className="field field--search">
            <span>Travel date</span>
            <div className="field__control">
              <FiCalendar />
              <input type="date" min={today} {...searchForm.register('travelDate')} />
            </div>
          </label>
          <button className="button button--primary" disabled={searchQuery.isFetching}>
            {searchQuery.isFetching ? <span className="button-spinner" /> : <FiSearch />}
            Search routes
          </button>
        </form>
        {searchParams && (
          <button className="text-button route-search-card__clear" onClick={() => setSearchParams(null)}>
            Clear search and show all routes
          </button>
        )}
      </section>

      {(routesQuery.isError || searchQuery.isError) && (
        <ErrorState
          error={searchQuery.error || routesQuery.error}
          onRetry={searchParams ? searchQuery.refetch : routesQuery.refetch}
        />
      )}

      <section className="panel">
        <div className="panel__header panel__header--bordered">
          <div>
            <span className="eyebrow">{searchParams ? 'Search results' : 'Complete network'}</span>
            <h3>
              {searchParams
                ? `${displayedRoutes.length} matching route${displayedRoutes.length === 1 ? '' : 's'}`
                : 'Routes and daily schedules'}
            </h3>
          </div>
        </div>

        {displayedRoutes.length ? (
          <div className="route-list">
            {displayedRoutes.map((route) => {
              const isSearchResult = 'availableSchedules' in route
              const schedules = isSearchResult ? route.availableSchedules : route.schedules
              const expanded = expandedRoute === route.routeId
              return (
                <article className="route-card" key={route.routeId}>
                  <div className="route-card__main">
                    <span className="route-code route-code--large">
                      R{String(route.routeId).padStart(2, '0')}
                    </span>
                    <div className="route-card__identity">
                      <div>
                        <h3>{route.routeName}</h3>
                        {!isSearchResult && <StatusBadge active={route.active} />}
                      </div>
                      <p>
                        <span>{isSearchResult ? route.sourceStopName : route.startPoint}</span>
                        <FiArrowRight />
                        <span>{isSearchResult ? route.destinationStopName : route.endPoint}</span>
                      </p>
                    </div>
                    <div className="route-card__meta">
                      <span>
                        <FiClock />
                        {schedules?.length || 0} schedule{schedules?.length === 1 ? '' : 's'}
                      </span>
                      {isSearchResult && (
                        <>
                          <span>{route.totalDistanceKm} km</span>
                          <strong>{formatCurrency(route.estimatedFare)}</strong>
                        </>
                      )}
                    </div>
                    <div className="route-card__actions">
                      {(['PASSENGER', 'CONDUCTOR', 'ADMIN'].includes(role)) && (
                        <Link
                          className="button button--primary button--small"
                          to={`/book?routeId=${route.routeId}${
                            isSearchResult
                              ? `&sourceStopId=${route.sourceStopId}&destinationStopId=${route.destinationStopId}`
                              : ''
                          }`}
                        >
                          Book
                        </Link>
                      )}
                      {canManage && !isSearchResult && (
                        <>                    
                        
                       <Link
                          className="button button--secondary button--small"
                          to={`/routes/${route.routeId}/stops`}
                        >
                          Stops
                        </Link>

                        <Link
                          className="button button--secondary button--small"
                          to={`/routes/${route.routeId}/schedules`}
                        >
                          Schedules
                        </Link>

                        <button
                          className="icon-button"
                          aria-label={`Edit ${route.routeName}`}
                          onClick={() => openEdit(route)}
                        >
                          <FiEdit2 />
                        </button>
                        </>                       
                      )}
                      {role === 'ADMIN' && !isSearchResult && (
                        <button
                          className="icon-button icon-button--danger"
                          aria-label={`Delete ${route.routeName}`}
                          onClick={() => handleDelete(route)}
                        >
                          <FiTrash2 />
                        </button>
                      )}
                      <button
                        className="icon-button"
                        aria-label={expanded ? 'Hide route details' : 'Show route details'}
                        onClick={() => setExpandedRoute(expanded ? null : route.routeId)}
                      >
                        {expanded ? <FiChevronUp /> : <FiChevronDown />}
                      </button>
                    </div>
                  </div>

                  {expanded && (
                    <div className="route-card__details">
                      <div>
                        <strong>Available departures</strong>
                        <div className="schedule-chips">
                          {schedules?.length ? (
                            schedules.map((schedule) => (
                              <span key={schedule.scheduleId}>
                                {formatTime(schedule.departureTime)}
                                <small>to {formatTime(schedule.arrivalTime)}</small>
                              </span>
                            ))
                          ) : (
                            <em>No active schedule</em>
                          )}
                        </div>
                      </div>
                      {!isSearchResult && (
                        <div>
                          <strong>Stops</strong>
                          <p>{route.stops?.length || 0} stops configured for this route.</p>
                        </div>
                      )}
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        ) : (
          <EmptyState
            icon={FiMapPin}
            title={searchParams ? 'No matching service found' : 'No routes configured'}
            description={
              searchParams
                ? 'Try a nearby stop name or another travel date.'
                : 'Create the first route to start building the transport network.'
            }
            action={
              canManage && !searchParams ? (
                <button className="button button--primary" onClick={openCreate}>
                  <FiPlus /> Add route
                </button>
              ) : null
            }
          />
        )}
      </section>

      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={editingRoute ? 'Edit route' : 'Create a new route'}
        description="Define the route identity and its network endpoints."
      >
        <form
          className="form-stack"
          onSubmit={routeForm.handleSubmit((values) => saveRoute.mutate(values))}
        >
          <label className="field">
            <span>Route name</span>
            <div className="field__control">
              <input
                placeholder="Airport Express"
                {...routeForm.register('routeName', { required: 'Route name is required' })}
              />
            </div>
            {routeForm.formState.errors.routeName && (
              <small className="field__error">{routeForm.formState.errors.routeName.message}</small>
            )}
          </label>
          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Start point</span>
              <div className="field__control">
                <FiMapPin />
                <input
                  placeholder="Central Station"
                  {...routeForm.register('startPoint', { required: 'Start point is required' })}
                />
              </div>
            </label>
            <label className="field">
              <span>End point</span>
              <div className="field__control">
                <FiMapPin />
                <input
                  placeholder="International Airport"
                  {...routeForm.register('endPoint', { required: 'End point is required' })}
                />
              </div>
            </label>
          </div>
          <div className="modal__actions">
            <button type="button" className="button button--ghost" onClick={closeModal}>
              Cancel
            </button>
            <button className="button button--primary" disabled={saveRoute.isPending}>
              {saveRoute.isPending && <span className="button-spinner" />}
              {editingRoute ? 'Save changes' : 'Create route'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
