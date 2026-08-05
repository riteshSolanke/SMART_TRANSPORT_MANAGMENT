import { useEffect, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import {
  FiArrowLeft,
  FiArrowRight,
  FiCalendar,
  FiCheckCircle,
  FiClock,
  FiInfo,
  FiMapPin,
  FiShield,
  FiUsers,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import ErrorState from '../components/ui/ErrorState.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import { useAuth } from '../context/authContext.js'
import { routesApi, ticketsApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import {
  createIdempotencyAttempt,
  formatCurrency,
  formatTime,
} from '../lib/formatters.js'

const today = new Date().toISOString().slice(0, 10)

export default function BookingPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { role } = useAuth()
  const bookingAttemptRef = useRef(createIdempotencyAttempt('booking'))
  const preselectedRoute = searchParams.get('routeId') || ''
  const { register, handleSubmit, control, setValue, formState: { errors } } = useForm({
    defaultValues: {
      routeId: preselectedRoute,
      scheduleId: '',
      sourceStopId: searchParams.get('sourceStopId') || '',
      destinationStopId: searchParams.get('destinationStopId') || '',
      serviceDate: today,
      passengerCount: 1,
    },
  })

  const [
    routeId,
    scheduleId,
    sourceStopId,
    destinationStopId,
    serviceDate,
    passengerCountValue,
  ] =
    useWatch({
      control,
      name: [
        'routeId',
        'scheduleId',
        'sourceStopId',
        'destinationStopId',
        'serviceDate',
        'passengerCount',
      ],
    })
  const passengerCount = Number(passengerCountValue || 1)

  const routesQuery = useQuery({ queryKey: ['routes'], queryFn: routesApi.all })
  const routeQuery = useQuery({
    queryKey: ['routes', routeId],
    queryFn: () => routesApi.get(routeId),
    enabled: Boolean(routeId),
  })
  const fareQuery = useQuery({
    queryKey: ['fare', routeId, sourceStopId, destinationStopId, scheduleId],
    queryFn: () =>
      routesApi.fare(routeId, {
        sourceStopId,
        destinationStopId,
        scheduleId: scheduleId || undefined,
      }),
    enabled: Boolean(routeId && sourceStopId && destinationStopId),
  })
  const availabilityQuery = useQuery({
    queryKey: ['tickets', 'availability', routeId, scheduleId, serviceDate],
    queryFn: () =>
      ticketsApi.availability({
        routeId: Number(routeId),
        scheduleId: Number(scheduleId),
        serviceDate,
      }),
    enabled: Boolean(routeId && scheduleId && serviceDate),
    staleTime: 10_000,
    retry: false,
    refetchOnWindowFocus: false,
  })
  const remainingSeats = availabilityQuery.data?.assigned
    ? Number(availabilityQuery.data.remainingSeats)
    : null
  const maxPassengerCount = remainingSeats === null
    ? 10
    : Math.max(0, Math.min(10, remainingSeats))
  const availabilityErrorMessage = availabilityQuery.isError
    ? getErrorMessage(
        availabilityQuery.error,
        'Unable to check seat availability. Please retry.',
      )
    : ''

  useEffect(() => {
    if (!routeQuery.data || scheduleId) return
    const firstSchedule = routeQuery.data.schedules?.find((schedule) => schedule.active)
    if (firstSchedule) setValue('scheduleId', String(firstSchedule.scheduleId))
  }, [routeQuery.data, scheduleId, setValue])

  useEffect(() => {
    if (maxPassengerCount > 0 && passengerCount > maxPassengerCount) {
      setValue('passengerCount', maxPassengerCount)
    }
  }, [maxPassengerCount, passengerCount, setValue])

  const booking = useMutation({
    mutationFn: (payload) =>
      ticketsApi.book(payload, bookingAttemptRef.current.keyFor(payload)),
    retry: (failureCount, error) =>
      failureCount < 1 && [502, 503, 504].includes(error?.response?.status),
    onSuccess: (ticket) => {
      bookingAttemptRef.current.reset()
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      queryClient.invalidateQueries({ queryKey: ['tickets', 'availability'] })
      toast.success('Ticket reserved. Complete payment before it expires.')
      navigate(`/payments?ticketId=${ticket.ticketId}&amount=${ticket.fareAmount}`, {
        replace: true,
      })
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to reserve this ticket')),
  })

  if (routesQuery.isLoading) return <PageLoader label="Preparing the booking journey" />
  if (routesQuery.isError) {
    return <ErrorState error={routesQuery.error} onRetry={routesQuery.refetch} />
  }

  const route = routeQuery.data
  const stops = route?.stops || []
  const sourceStop = stops.find((stop) => String(stop.stopId) === String(sourceStopId))
  const destinationStop = stops.find(
    (stop) => String(stop.stopId) === String(destinationStopId),
  )
  const selectedSchedule = route?.schedules?.find(
    (schedule) => String(schedule.scheduleId) === String(scheduleId),
  )
  const totalFare = Number(fareQuery.data?.fare || 0) * passengerCount

  function submit(values) {
    if (String(values.sourceStopId) === String(values.destinationStopId)) {
      toast.error('Origin and destination must be different stops')
      return
    }
    booking.mutate({
      routeId: Number(values.routeId),
      scheduleId: Number(values.scheduleId),
      sourceStopId: Number(values.sourceStopId),
      destinationStopId: Number(values.destinationStopId),
      serviceDate: values.serviceDate,
      passengerCount: Number(values.passengerCount),
    })
  }

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow={role === 'CONDUCTOR' ? 'Conductor ticketing' : 'Passenger booking'}
        title={role === 'CONDUCTOR' ? 'Issue a walk-up ticket' : 'Build your journey'}
        description={
          role === 'CONDUCTOR'
            ? 'No passenger account or mobile number is required. Issue the PNR and collect the fare.'
            : 'Choose the service details below. Your fare is calculated by the route service.'
        }
        actions={
          <Link className="button button--ghost" to="/routes">
            <FiArrowLeft /> Back to routes
          </Link>
        }
      />

      <div className="booking-stepper">
        <span className="active"><strong>1</strong> Journey</span>
        <i />
        <span><strong>2</strong> Payment</span>
        <i />
        <span><strong>3</strong> Ticket</span>
      </div>

      <form className="booking-layout" onSubmit={handleSubmit(submit)}>
        <section className="panel booking-form">
          <div className="panel__header panel__header--bordered">
            <div>
              <span className="eyebrow">Journey details</span>
              <h3>Select your service</h3>
            </div>
            <span className="secure-pill">
              <FiShield /> {role === 'CONDUCTOR' ? 'Conductor issued' : 'Secure reservation'}
            </span>
          </div>

          <div className="form-stack">
            <label className="field">
              <span>Route</span>
              <div className="field__control">
                <FiMapPin />
                <select
                  {...register('routeId', { required: 'Select a route' })}
                  onChange={(event) => {
                    setValue('routeId', event.target.value)
                    setValue('scheduleId', '')
                    setValue('sourceStopId', '')
                    setValue('destinationStopId', '')
                  }}
                >
                  <option value="">Choose a route</option>
                  {(routesQuery.data || [])
                    .filter((item) => item.active)
                    .map((item) => (
                      <option value={item.routeId} key={item.routeId}>
                        {item.routeName} · {item.startPoint} to {item.endPoint}
                      </option>
                    ))}
                </select>
              </div>
              {errors.routeId && <small className="field__error">{errors.routeId.message}</small>}
            </label>

            {routeQuery.isFetching && <div className="form-skeleton" />}
            {routeQuery.isError && <ErrorState error={routeQuery.error} onRetry={routeQuery.refetch} compact />}

            {route && (
              <>
                <div className="form-grid form-grid--two">
                  <label className="field">
                    <span>Boarding stop</span>
                    <div className="field__control">
                      <FiMapPin />
                      <select
                        {...register('sourceStopId', { required: 'Select a boarding stop' })}
                      >
                        <option value="">Choose origin</option>
                        {stops.map((stop) => (
                          <option value={stop.stopId} key={stop.stopId}>
                            {stop.sequenceOrder}. {stop.stopName}
                          </option>
                        ))}
                      </select>
                    </div>
                    {errors.sourceStopId && (
                      <small className="field__error">{errors.sourceStopId.message}</small>
                    )}
                  </label>
                  <label className="field">
                    <span>Destination stop</span>
                    <div className="field__control">
                      <FiMapPin />
                      <select
                        {...register('destinationStopId', {
                          required: 'Select a destination stop',
                        })}
                      >
                        <option value="">Choose destination</option>
                        {stops.map((stop) => (
                          <option value={stop.stopId} key={stop.stopId}>
                            {stop.sequenceOrder}. {stop.stopName}
                          </option>
                        ))}
                      </select>
                    </div>
                    {errors.destinationStopId && (
                      <small className="field__error">{errors.destinationStopId.message}</small>
                    )}
                  </label>
                </div>

                <div className="form-grid form-grid--three">
                  <label className="field">
                    <span>Schedule</span>
                    <div className="field__control">
                      <FiClock />
                      <select {...register('scheduleId', { required: 'Select a schedule' })}>
                        <option value="">Departure</option>
                        {route.schedules
                          ?.filter((schedule) => schedule.active)
                          .map((schedule) => (
                            <option value={schedule.scheduleId} key={schedule.scheduleId}>
                              {formatTime(schedule.departureTime)} – {formatTime(schedule.arrivalTime)}
                            </option>
                          ))}
                      </select>
                    </div>
                  </label>
                  <label className="field">
                    <span>Travel date</span>
                    <div className="field__control">
                      <FiCalendar />
                      <input
                        type="date"
                        min={today}
                        {...register('serviceDate', { required: 'Select a travel date' })}
                      />
                    </div>
                  </label>
                  <label className="field">
                    <span>Passengers</span>
                    <div className="field__control">
                      <FiUsers />
                      <select {...register('passengerCount')}>
                        {Array.from(
                          { length: Math.max(maxPassengerCount, 1) },
                          (_, index) => index + 1,
                        ).map((count) => (
                          <option value={count} key={count}>{count}</option>
                        ))}
                      </select>
                    </div>
                  </label>
                </div>
              </>
            )}
          </div>
        </section>

        <aside className="panel booking-summary">
          <span className="eyebrow">Booking summary</span>
          <h3>{route?.routeName || 'Select a route'}</h3>
          <div className="booking-summary__route">
            <span />
            <div>
              <strong>{sourceStop?.stopName || 'Boarding stop'}</strong>
              <small>{selectedSchedule ? formatTime(selectedSchedule.departureTime) : 'Select time'}</small>
            </div>
            <i />
            <span />
            <div>
              <strong>{destinationStop?.stopName || 'Destination'}</strong>
              <small>{selectedSchedule ? formatTime(selectedSchedule.arrivalTime) : 'Arrival time'}</small>
            </div>
          </div>

          <div className="booking-summary__details">
            <span><FiUsers /> Passengers <strong>{passengerCount}</strong></span>
            <span>
              <FiUsers /> Seats available
              <strong>
                {availabilityQuery.isFetching
                  ? 'Checking...'
                  : availabilityQuery.isError
                    ? 'Check failed'
                    : availabilityQuery.data?.assigned
                      ? `${availabilityQuery.data.remainingSeats} of ${availabilityQuery.data.capacity}`
                      : scheduleId
                        ? 'No vehicle assigned'
                        : 'Select schedule'}
              </strong>
            </span>
            <span>
              <FiMapPin /> Distance
              <strong>{fareQuery.data?.distanceKm ? `${fareQuery.data.distanceKm} km` : '—'}</strong>
            </span>
            {fareQuery.data?.peakHourApplied && (
              <span className="booking-summary__notice">
                <FiInfo /> Peak-hour fare applied
              </span>
            )}
          </div>
          {availabilityQuery.isError && (
            <div>
              <p className="field__error">{availabilityErrorMessage}</p>
              <button
                type="button"
                className="button button--ghost button--small"
                onClick={() => availabilityQuery.refetch()}
              >
                Retry seat check
              </button>
            </div>
          )}
          <div className="booking-summary__fare">
            <div>
              <span>Total fare</span>
              <small>Inclusive of all applicable charges</small>
            </div>
            {fareQuery.isFetching ? (
              <span className="price-skeleton" />
            ) : (
              <strong>{formatCurrency(totalFare)}</strong>
            )}
          </div>
          <button
            className="button button--primary button--block"
            disabled={
              booking.isPending ||
              !fareQuery.data ||
              !scheduleId ||
              availabilityQuery.isFetching ||
              !availabilityQuery.data?.available ||
              (remainingSeats !== null && passengerCount > remainingSeats)
            }
          >
            {booking.isPending ? <span className="button-spinner" /> : <FiCheckCircle />}
            Reserve and continue
            {!booking.isPending && <FiArrowRight />}
          </button>
          <p className="booking-summary__secure">
            <FiShield /> {role === 'CONDUCTOR'
              ? 'Give the generated PNR to the passenger after collecting payment.'
              : 'Your seat is held temporarily while payment is completed.'}
          </p>
        </aside>
      </form>
    </div>
  )
}
