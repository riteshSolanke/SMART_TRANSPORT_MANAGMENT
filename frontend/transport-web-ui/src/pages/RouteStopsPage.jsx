import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  FiArrowLeft,
  FiEdit2,
  FiMapPin,
  FiPlus,
  FiTrash2,
} from 'react-icons/fi'
import toast from 'react-hot-toast'

import SectionHeader from '../components/ui/SectionHeader.jsx'
import Modal from '../components/ui/Modal.jsx'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'

import { routesApi, stopsApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { confirmAction } from '../lib/alerts.js'

export default function RouteStopsPage() {
  const { routeId } = useParams()
  const queryClient = useQueryClient()

  const [modalOpen, setModalOpen] = useState(false)
  const [editingStop, setEditingStop] = useState(null)

  const form = useForm({
    defaultValues: {
      stopName: '',
      sequenceOrder: '',
      distanceFromStart: '',
    },
  })

  const routeQuery = useQuery({
    queryKey: ['route', routeId],
    queryFn: () => routesApi.get(routeId),
  })

  const stopsQuery = useQuery({
    queryKey: ['stops', routeId],
    queryFn: () => stopsApi.all(routeId),
  })

  const saveMutation = useMutation({
    mutationFn: (payload) =>
      editingStop
        ? stopsApi.update(routeId, editingStop.stopId, payload)
        : stopsApi.create(routeId, payload),

    onSuccess: () => {
      toast.success(
        editingStop
          ? 'Stop updated successfully'
          : 'Stop created successfully',
      )

      queryClient.invalidateQueries({
        queryKey: ['stops', routeId],
      })

      queryClient.invalidateQueries({
        queryKey: ['route', routeId],
      })

      closeModal()
    },

    onError: (error) => {
      toast.error(getErrorMessage(error))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (stopId) => stopsApi.remove(routeId, stopId),

    onSuccess: () => {
      toast.success('Stop deleted successfully')

      queryClient.invalidateQueries({
        queryKey: ['stops', routeId],
      })
    },

    onError: (error) => {
      toast.error(getErrorMessage(error))
    },
  })

  function openCreate() {
    setEditingStop(null)

    form.reset({
      stopName: '',
      sequenceOrder: '',
      distanceFromStart: '',
    })

    setModalOpen(true)
  }

  function openEdit(stop) {
    setEditingStop(stop)

    form.reset({
      stopName: stop.stopName,
      sequenceOrder: stop.sequenceOrder,
      distanceFromStart: stop.distanceFromStart,
    })

    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingStop(null)
  }

  async function handleDelete(stop) {
    const result = await confirmAction({
      title: `Delete ${stop.stopName}?`,
      text: 'This stop will be removed from the route.',
      confirmText: 'Delete',
      confirmColor: '#dc2626',
    })

    if (result.isConfirmed) {
      deleteMutation.mutate(stop.stopId)
    }
  }

  if (routeQuery.isLoading || stopsQuery.isLoading) {
    return <PageLoader label="Loading stops" />
  }

  if (routeQuery.isError || stopsQuery.isError) {
    return (
      <ErrorState
        error={routeQuery.error || stopsQuery.error}
      />
    )
  }

  const route = routeQuery.data
  const stops = stopsQuery.data || []

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Route Management"
        title={`${route.routeName} Stops`}
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
              Add Stop
            </button>
          </div>
        }
      />

      {stops.length ? (
        <section className="panel">
          <div className="route-list">
            {stops.map((stop) => (
              <article className="route-card" key={stop.stopId}>
                <div className="route-card__main">
                  <span className="route-code">
                    #{stop.sequenceOrder}
                  </span>

                  <div className="route-card__identity">
                    <h3>{stop.stopName}</h3>

                    <p>
                      <FiMapPin />
                      {stop.distanceFromStart} KM from route
                      start
                    </p>
                  </div>

                  <div className="route-card__actions">
                    <button
                      className="icon-button"
                      onClick={() => openEdit(stop)}
                    >
                      <FiEdit2 />
                    </button>

                    <button
                      className="icon-button icon-button--danger"
                      onClick={() => handleDelete(stop)}
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
          title="No stops configured"
          description="Add the first stop to this route."
        />
      )}

      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={editingStop ? 'Edit Stop' : 'Create Stop'}
      >
        <form
          className="form-stack"
          onSubmit={form.handleSubmit((values) =>
            saveMutation.mutate({
              ...values,
              sequenceOrder: Number(values.sequenceOrder),
              distanceFromStart: Number(values.distanceFromStart),
            }),
          )}
        >
          <label className="field">
            <span>Stop Name</span>
            <div className="field__control">
              <input
                {...form.register('stopName', {
                  required: true,
                })}
              />
            </div>
          </label>

          <label className="field">
            <span>Sequence Order</span>
            <div className="field__control">
              <input
                type="number"
                {...form.register('sequenceOrder', {
                  required: true,
                })}
              />
            </div>
          </label>

          <label className="field">
            <span>Distance From Start</span>
            <div className="field__control">
              <input
                type="number"
                step="0.01"
                {...form.register('distanceFromStart', {
                  required: true,
                })}
              />
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