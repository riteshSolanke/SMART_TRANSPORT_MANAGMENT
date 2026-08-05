import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import {
  FiCalendar,
  FiCheck,
  FiClock,
  FiMapPin,
  FiMoreVertical,
  FiNavigation,
  FiPlus,
  FiTrash2,
  FiTruck,
  FiUsers,
  FiX,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import Modal from '../components/ui/Modal.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import StatCard from '../components/ui/StatCard.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { useAuth } from '../context/authContext.js'
import { confirmAction } from '../lib/alerts.js'
import { routesApi, vehiclesApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { formatDate, formatTime, humanize } from '../lib/formatters.js'

const statuses = ['AVAILABLE', 'IN_SERVICE', 'MAINTENANCE']
const today = new Date().toISOString().slice(0, 10)

export default function FleetPage() {
  const { role } = useAuth()
  const queryClient = useQueryClient()
  const canCreate = ['TRANSPORT_MANAGER', 'ADMIN'].includes(role)
  const canOperate = ['DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'].includes(role)
  const canTrack = ['CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'].includes(role)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [vehicleModal, setVehicleModal] = useState(false)
  const [assignmentVehicle, setAssignmentVehicle] = useState(null)
  const [historyVehicle, setHistoryVehicle] = useState(null)
  const [locationVehicle, setLocationVehicle] = useState(null)

  const vehicleForm = useForm({ defaultValues: { vehicleNumber: '', capacity: 40 } })
  const assignmentForm = useForm({
    defaultValues: { routeId: '', scheduleId: '', serviceDate: today },
  })
  const locationForm = useForm({
    defaultValues: { latitude: '19.076000', longitude: '72.877700', speedKph: '0' },
  })

  const vehiclesQuery = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesApi.all })
  const routesQuery = useQuery({
    queryKey: ['routes', 'detailed'],
    queryFn: routesApi.allDetailed,
  })
  const assignmentsQuery = useQuery({
    queryKey: ['vehicles', historyVehicle?.vehicleId, 'assignments'],
    queryFn: () => vehiclesApi.assignments(historyVehicle.vehicleId),
    enabled: Boolean(historyVehicle),
  })

  const selectedRouteId = useWatch({
    control: assignmentForm.control,
    name: 'routeId',
  })
  const selectedRoute = (routesQuery.data || []).find(
    (route) => String(route.routeId) === String(selectedRouteId),
  )

  const createVehicle = useMutation({
    mutationFn: vehiclesApi.create,
    onSuccess: () => {
      toast.success('Vehicle added to the fleet')
      vehicleForm.reset()
      setVehicleModal(false)
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const updateStatus = useMutation({
    mutationFn: ({ vehicleId, status }) => vehiclesApi.updateStatus(vehicleId, status),
    onSuccess: () => {
      toast.success('Vehicle status updated')
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const assignVehicle = useMutation({
    mutationFn: (payload) => vehiclesApi.assign(assignmentVehicle.vehicleId, payload),
    onSuccess: () => {
      toast.success('Vehicle assigned successfully')
      setAssignmentVehicle(null)
      assignmentForm.reset({ routeId: '', scheduleId: '', serviceDate: today })
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const recordLocation = useMutation({
    mutationFn: (payload) => vehiclesApi.recordLocation(locationVehicle.vehicleId, payload),
    onSuccess: () => {
      toast.success('Simulated vehicle location recorded')
      setLocationVehicle(null)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const completeAssignment = useMutation({
    mutationFn: ({ vehicleId, assignmentId }) =>
      vehiclesApi.completeAssignment(vehicleId, assignmentId),
    onSuccess: () => {
      toast.success('Assignment marked complete')
      assignmentsQuery.refetch()
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const cancelAssignment = useMutation({
    mutationFn: ({ vehicleId, assignmentId }) =>
      vehiclesApi.cancelAssignment(vehicleId, assignmentId),
    onSuccess: () => {
      toast.success('Assignment cancelled')
      assignmentsQuery.refetch()
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const deleteVehicle = useMutation({
    mutationFn: vehiclesApi.remove,
    onSuccess: () => {
      toast.success('Vehicle removed')
      queryClient.invalidateQueries({ queryKey: ['vehicles'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const vehicles = useMemo(() => vehiclesQuery.data || [], [vehiclesQuery.data])
  const filtered = useMemo(
    () =>
      statusFilter === 'ALL'
        ? vehicles
        : vehicles.filter((vehicle) => vehicle.status === statusFilter),
    [vehicles, statusFilter],
  )

  async function removeVehicle(vehicle) {
    const result = await confirmAction({
      title: `Remove ${vehicle.vehicleNumber}?`,
      text: 'This vehicle will no longer be available for assignment.',
      confirmText: 'Remove vehicle',
      confirmColor: '#dc2626',
    })
    if (result.isConfirmed) deleteVehicle.mutate(vehicle.vehicleId)
  }

  async function changeAssignment(assignment, action) {
    const result = await confirmAction({
      title: `${action === 'complete' ? 'Complete' : 'Cancel'} this assignment?`,
      text: `Assignment #${assignment.assignmentId} will be marked ${action}d.`,
      confirmText: action === 'complete' ? 'Mark complete' : 'Cancel assignment',
      icon: action === 'complete' ? 'question' : 'warning',
      confirmColor: action === 'complete' ? '#0d9488' : '#dc2626',
    })
    if (!result.isConfirmed) return
    const payload = {
      vehicleId: assignment.vehicleId,
      assignmentId: assignment.assignmentId,
    }
    if (action === 'complete') completeAssignment.mutate(payload)
    else cancelAssignment.mutate(payload)
  }

  if (vehiclesQuery.isLoading) return <PageLoader label="Checking every vehicle in the fleet" />
  if (vehiclesQuery.isError) {
    return <ErrorState error={vehiclesQuery.error} onRetry={vehiclesQuery.refetch} />
  }

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Operations control"
        title="Fleet readiness"
        description="Review capacity and service status, then coordinate assignments and simulated tracking."
        actions={
          canCreate && (
            <button className="button button--primary" onClick={() => setVehicleModal(true)}>
              <FiPlus /> Add vehicle
            </button>
          )
        }
      />

      <section className="stat-grid stat-grid--four">
        <StatCard label="Total fleet" value={vehicles.length} icon={FiTruck} tone="blue" />
        <StatCard
          label="Available"
          value={vehicles.filter((vehicle) => vehicle.status === 'AVAILABLE').length}
          icon={FiCheck}
          tone="teal"
        />
        <StatCard
          label="In service"
          value={vehicles.filter((vehicle) => vehicle.status === 'IN_SERVICE').length}
          icon={FiNavigation}
          tone="amber"
        />
        <StatCard
          label="Total capacity"
          value={vehicles.reduce((sum, vehicle) => sum + vehicle.capacity, 0)}
          icon={FiUsers}
          tone="violet"
        />
      </section>

      <section className="panel">
        <div className="panel__header panel__header--bordered fleet-toolbar">
          <div>
            <span className="eyebrow">Vehicle registry</span>
            <h3>Service vehicles</h3>
          </div>
          <div className="segmented-control">
            {['ALL', ...statuses].map((status) => (
              <button
                className={statusFilter === status ? 'active' : ''}
                onClick={() => setStatusFilter(status)}
                key={status}
              >
                {humanize(status)}
              </button>
            ))}
          </div>
        </div>

        {filtered.length ? (
          <div className="fleet-grid">
            {filtered.map((vehicle) => (
              <article className="vehicle-card" key={vehicle.vehicleId}>
                <header>
                  <span className="vehicle-card__icon"><FiTruck /></span>
                  <div>
                    <strong>{vehicle.vehicleNumber}</strong>
                    <small>Vehicle #{vehicle.vehicleId}</small>
                  </div>
                  <button className="icon-button" aria-label="Vehicle options">
                    <FiMoreVertical />
                  </button>
                </header>
                <div className="vehicle-card__status">
                  <StatusBadge status={vehicle.status} />
                  <span>{vehicle.active ? 'Active registry' : 'Inactive registry'}</span>
                </div>
                <div className="vehicle-card__capacity">
                  <FiUsers />
                  <div>
                    <span>Passenger capacity</span>
                    <strong>{vehicle.capacity} seats</strong>
                  </div>
                </div>
                {canOperate && (
                  <label className="vehicle-card__select">
                    <span>Service status</span>
                    <select
                      value={vehicle.status}
                      disabled={updateStatus.isPending}
                      onChange={(event) =>
                        updateStatus.mutate({
                          vehicleId: vehicle.vehicleId,
                          status: event.target.value,
                        })
                      }
                    >
                      {statuses.map((status) => (
                        <option value={status} key={status}>{humanize(status)}</option>
                      ))}
                    </select>
                  </label>
                )}
                <footer>
                  {canOperate && (
                    <button
                      className="button button--primary button--small"
                      onClick={() => setAssignmentVehicle(vehicle)}
                    >
                      Assign
                    </button>
                  )}
                  {canOperate && (
                    <button
                      className="button button--ghost button--small"
                      onClick={() => setHistoryVehicle(vehicle)}
                    >
                      History
                    </button>
                  )}
                  {canTrack && (
                    <button
                      className="icon-button"
                      title="Record simulated location"
                      onClick={() => setLocationVehicle(vehicle)}
                    >
                      <FiMapPin />
                    </button>
                  )}
                  {role === 'ADMIN' && (
                    <button
                      className="icon-button icon-button--danger"
                      title="Remove vehicle"
                      onClick={() => removeVehicle(vehicle)}
                    >
                      <FiTrash2 />
                    </button>
                  )}
                </footer>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={FiTruck}
            title="No vehicles match this filter"
            description="Choose another status or add a vehicle to the fleet."
          />
        )}
      </section>

      <Modal
        open={vehicleModal}
        onClose={() => setVehicleModal(false)}
        title="Add fleet vehicle"
        description="Register a vehicle and its safe passenger capacity."
      >
        <form
          className="form-stack"
          onSubmit={vehicleForm.handleSubmit((values) =>
            createVehicle.mutate({
              vehicleNumber: values.vehicleNumber.trim(),
              capacity: Number(values.capacity),
            }),
          )}
        >
          <label className="field">
            <span>Vehicle number</span>
            <div className="field__control">
              <FiTruck />
              <input
                placeholder="MH-01-AB-1234"
                {...vehicleForm.register('vehicleNumber', { required: true })}
              />
            </div>
          </label>
          <label className="field">
            <span>Passenger capacity</span>
            <div className="field__control">
              <FiUsers />
              <input
                type="number"
                min="1"
                max="500"
                {...vehicleForm.register('capacity', { required: true })}
              />
            </div>
          </label>
          <div className="modal__actions">
            <button type="button" className="button button--ghost" onClick={() => setVehicleModal(false)}>
              Cancel
            </button>
            <button className="button button--primary" disabled={createVehicle.isPending}>
              {createVehicle.isPending && <span className="button-spinner" />}
              Add vehicle
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(assignmentVehicle)}
        onClose={() => setAssignmentVehicle(null)}
        title={`Assign ${assignmentVehicle?.vehicleNumber || 'vehicle'}`}
        description="Select an active route, departure and service date."
      >
        <form
          className="form-stack"
          onSubmit={assignmentForm.handleSubmit((values) =>
            assignVehicle.mutate({
              routeId: Number(values.routeId),
              scheduleId: Number(values.scheduleId),
              serviceDate: values.serviceDate,
            }),
          )}
        >
          <label className="field">
            <span>Route</span>
            <div className="field__control">
              <FiNavigation />
              <select
                {...assignmentForm.register('routeId', { required: true })}
                onChange={(event) => {
                  assignmentForm.setValue('routeId', event.target.value)
                  assignmentForm.setValue('scheduleId', '')
                }}
              >
                <option value="">Choose route</option>
                {(routesQuery.data || []).filter((route) => route.active).map((route) => (
                  <option value={route.routeId} key={route.routeId}>{route.routeName}</option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span>Schedule</span>
            <div className="field__control">
              <FiClock />
              <select {...assignmentForm.register('scheduleId', { required: true })}>
                <option value="">Choose departure</option>
                {selectedRoute?.schedules?.filter((schedule) => schedule.active).map((schedule) => (
                  <option value={schedule.scheduleId} key={schedule.scheduleId}>
                    {formatTime(schedule.departureTime)} – {formatTime(schedule.arrivalTime)}
                  </option>
                ))}
              </select>
            </div>
          </label>
          <label className="field">
            <span>Service date</span>
            <div className="field__control">
              <FiCalendar />
              <input
                type="date"
                min={today}
                {...assignmentForm.register('serviceDate', { required: true })}
              />
            </div>
          </label>
          <div className="modal__actions">
            <button type="button" className="button button--ghost" onClick={() => setAssignmentVehicle(null)}>
              Cancel
            </button>
            <button className="button button--primary" disabled={assignVehicle.isPending}>
              {assignVehicle.isPending && <span className="button-spinner" />}
              Confirm assignment
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(historyVehicle)}
        onClose={() => setHistoryVehicle(null)}
        title={`${historyVehicle?.vehicleNumber || ''} assignments`}
        description="Latest operational assignment history."
        size="large"
      >
        {assignmentsQuery.isLoading ? (
          <PageLoader label="Loading assignment history" />
        ) : assignmentsQuery.isError ? (
          <ErrorState error={assignmentsQuery.error} onRetry={assignmentsQuery.refetch} compact />
        ) : assignmentsQuery.data?.length ? (
          <div className="assignment-list">
            {assignmentsQuery.data.map((assignment) => {
              const assignmentRoute = (routesQuery.data || []).find(
                (route) => String(route.routeId) === String(assignment.routeId),
              )
              const assignmentSchedule = assignmentRoute?.schedules?.find(
                (schedule) =>
                  String(schedule.scheduleId) === String(assignment.scheduleId),
              )

              return (
                <article key={assignment.assignmentId}>
                  <span className="assignment-list__icon"><FiNavigation /></span>
                  <div>
                    <strong>
                      {assignmentRoute?.routeName || `Route #${assignment.routeId}`}
                      {' · '}
                      Schedule #{assignment.scheduleId}
                    </strong>
                    <span>
                      {formatDate(assignment.serviceDate)}
                      {' · '}
                      {assignmentSchedule
                        ? `${formatTime(assignmentSchedule.departureTime)} – ${formatTime(assignmentSchedule.arrivalTime)}`
                        : 'Departure time unavailable'}
                      {' · '}Assigned {formatDate(assignment.assignedAt, 'dd MMM, hh:mm a')}
                    </span>
                  </div>
                  <StatusBadge status={assignment.status} />
                  {assignment.status === 'ACTIVE' && (
                    <div className="assignment-list__actions">
                      <button className="icon-button icon-button--success" onClick={() => changeAssignment(assignment, 'complete')} title="Complete">
                        <FiCheck />
                      </button>
                      <button className="icon-button icon-button--danger" onClick={() => changeAssignment(assignment, 'cancel')} title="Cancel">
                        <FiX />
                      </button>
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        ) : (
          <EmptyState title="No assignment history" description="Assignments for this vehicle will appear here." />
        )}
      </Modal>

      <Modal
        open={Boolean(locationVehicle)}
        onClose={() => setLocationVehicle(null)}
        title={`Record ${locationVehicle?.vehicleNumber || ''} location`}
        description="GPS is simulated in this training environment."
      >
        <form
          className="form-stack"
          onSubmit={locationForm.handleSubmit((values) =>
            recordLocation.mutate({
              latitude: Number(values.latitude),
              longitude: Number(values.longitude),
              speedKph: Number(values.speedKph),
              recordedAt: new Date().toISOString().slice(0, 19),
            }),
          )}
        >
          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Latitude</span>
              <div className="field__control"><input type="number" step="0.000001" {...locationForm.register('latitude')} /></div>
            </label>
            <label className="field">
              <span>Longitude</span>
              <div className="field__control"><input type="number" step="0.000001" {...locationForm.register('longitude')} /></div>
            </label>
          </div>
          <label className="field">
            <span>Speed (km/h)</span>
            <div className="field__control">
              <FiNavigation />
              <input type="number" min="0" max="250" step="0.1" {...locationForm.register('speedKph')} />
            </div>
          </label>
          <div className="modal__actions">
            <button type="button" className="button button--ghost" onClick={() => setLocationVehicle(null)}>Cancel</button>
            <button className="button button--primary" disabled={recordLocation.isPending}>
              {recordLocation.isPending && <span className="button-spinner" />}
              Record location
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
