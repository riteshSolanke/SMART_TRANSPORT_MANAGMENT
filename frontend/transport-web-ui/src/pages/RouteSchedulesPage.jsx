import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import {
  FiArrowLeft,
  FiClock,
  FiEdit2,
  FiPlus,
  FiTrash2,
} from 'react-icons/fi'
import toast from 'react-hot-toast'

import SectionHeader from '../components/ui/SectionHeader.jsx'
import EmptyState from '../components/ui/EmptyState.jsx'
import Modal from '../components/ui/Modal.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

import { routesApi, schedulesApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { confirmAction } from '../lib/alerts.js'
import { formatTime } from '../lib/formatters.js'

const DAYS = [
  { value: 'MON', label: 'Monday' },
  { value: 'TUE', label: 'Tuesday' },
  { value: 'WED', label: 'Wednesday' },
  { value: 'THU', label: 'Thursday' },
  { value: 'FRI', label: 'Friday' },
  { value: 'SAT', label: 'Saturday' },
  { value: 'SUN', label: 'Sunday' },
]

export default function RouteSchedulesPage() {
  const { routeId } = useParams()

  const queryClient = useQueryClient()

  const [modalOpen, setModalOpen] = useState(false)
  const [editingSchedule, setEditingSchedule] = useState(null)
  const [selectedDays, setSelectedDays] = useState([])

  const form = useForm({
    defaultValues: {
      departureTime: '',
      arrivalTime: '',
      daysOfWeek: '',
    },
  })

  const routeQuery = useQuery({
    queryKey: ['route', routeId],
    queryFn: () => routesApi.get(routeId),
  })

  const schedulesQuery = useQuery({
    queryKey: ['schedules', routeId],
    queryFn: () => schedulesApi.all(routeId),
  })

  const saveMutation = useMutation({
    mutationFn: (payload) =>
      editingSchedule
        ? schedulesApi.update(
            routeId,
            editingSchedule.scheduleId,
            payload,
          )
        : schedulesApi.create(routeId, payload),

    onSuccess: () => {
      toast.success('Schedule saved')

      queryClient.invalidateQueries({
        queryKey: ['schedules', routeId],
      })

      closeModal()
    },

    onError: (error) => {
      toast.error(getErrorMessage(error))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (scheduleId) =>
      schedulesApi.remove(routeId, scheduleId),

    onSuccess: () => {
      toast.success('Schedule deleted')

      queryClient.invalidateQueries({
        queryKey: ['schedules', routeId],
      })
    },
  })

 function openCreate() {
  setEditingSchedule(null)

  form.reset({
    departureTime: '',
    arrivalTime: '',
  })

  setSelectedDays([])

  setModalOpen(true)
}
 function openEdit(schedule) {
  setEditingSchedule(schedule)

  form.reset({
    departureTime: schedule.departureTime,
    arrivalTime: schedule.arrivalTime,
  })

  setSelectedDays(
    schedule.daysOfWeek
      ? schedule.daysOfWeek.split(',')
      : [],
  )

  setModalOpen(true)
}

 function closeModal() {
  setModalOpen(false)
  setEditingSchedule(null)
  setSelectedDays([])
}

  function toggleDay(day) {
  setSelectedDays((current) =>
    current.includes(day)
      ? current.filter((d) => d !== day)
      : [...current, day],
  )
}

  async function handleDelete(schedule) {
    const result = await confirmAction({
      title: 'Delete Schedule?',
      confirmText: 'Delete',
      confirmColor: '#dc2626',
    })

    if (result.isConfirmed) {
      deleteMutation.mutate(schedule.scheduleId)
    }
  }

  if (routeQuery.isLoading || schedulesQuery.isLoading) {
    return <PageLoader label="Loading schedules" />
  }

  const route = routeQuery.data
  const schedules = schedulesQuery.data || []

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Schedule Management"
        title={`${route.routeName} Schedules`}
        description={`${route.startPoint} → ${route.endPoint}`}
        actions={
          <div style={{ display: 'flex', gap: '12px' }}>
            <Link
              className="button button--secondary"
              to="/routes"
            >
              <FiArrowLeft />
              Back
            </Link>

            <button
              className="button button--primary"
              onClick={openCreate}
            >
              <FiPlus />
              Add Schedule
            </button>
          </div>
        }
      />

      {schedules.length ? (
        <section className="panel">
          <div className="route-list">
            {schedules.map((schedule) => (
              <article
                className="route-card"
                key={schedule.scheduleId}
              >
                <div className="route-card__main">
                  <div className="route-card__identity">
                    <h3>
                      {formatTime(schedule.departureTime)}
                      {' → '}
                      {formatTime(schedule.arrivalTime)}
                    </h3>

                    <p>{schedule.daysOfWeek}</p>

                    <StatusBadge
                      active={schedule.active}
                    />
                  </div>

                  <div className="route-card__actions">
                    <button
                      className="icon-button"
                      onClick={() =>
                        openEdit(schedule)
                      }
                    >
                      <FiEdit2 />
                    </button>

                    <button
                      className="icon-button icon-button--danger"
                      onClick={() =>
                        handleDelete(schedule)
                      }
                    >
                      <FiTrash2 />
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : (
        <EmptyState
          icon={FiClock}
          title="No schedules configured"
          description="Add a schedule for this route."
        />
      )}

      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={
          editingSchedule
            ? 'Edit Schedule'
            : 'Create Schedule'
        }
      >
        <form
          className="form-stack"
         onSubmit={form.handleSubmit((values) => {
  if (!selectedDays.length) {
    toast.error('Please select at least one operating day')
    return
  }

  saveMutation.mutate({
    ...values,
    daysOfWeek: selectedDays.join(','),
  })
})}
        >
          <label className="field">
            <span>Departure Time</span>
            <div className="field__control">
              <input
                type="time"
                {...form.register('departureTime', {
                  required: true,
                })}
              />
            </div>
          </label>

          <label className="field">
            <span>Arrival Time</span>
            <div className="field__control">
              <input
                type="time"
                {...form.register('arrivalTime', {
                  required: true,
                })}
              />
            </div>
          </label>

     <label className="field">
  <span>Operating Days</span>

  <div
    style={{
      display: 'grid',
      gridTemplateColumns:
        'repeat(auto-fit, minmax(140px, 1fr))',
      gap: '12px',
      marginTop: '8px',
    }}
  >
    {DAYS.map((day) => (
      <label
        key={day.value}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '10px',
          border: '1px solid var(--border-color, #ddd)',
          borderRadius: '10px',
          cursor: 'pointer',
        }}
      >
        <input
          type="checkbox"
          checked={selectedDays.includes(day.value)}
          onChange={() => toggleDay(day.value)}
        />

        <span>{day.label}</span>
      </label>
    ))}
  </div>
</label>

          <div className="modal__actions">
            <button
              type="button"
              className="button button--ghost"
              onClick={closeModal}
            >
              Cancel
            </button>

            <button
              className="button button--primary"
              disabled={saveMutation.isPending}
            >
              Save
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
