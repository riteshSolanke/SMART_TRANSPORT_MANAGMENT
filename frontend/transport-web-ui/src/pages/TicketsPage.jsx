import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  FiCalendar,
  FiClock,
  FiCreditCard,
  FiSearch,
  FiTag,
  FiTrash2,
  FiUsers,
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
import { ticketsApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { formatCurrency, formatDate, formatTime } from '../lib/formatters.js'

const tabs = [
  { id: 'all', label: 'All tickets' },
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'payment', label: 'Awaiting payment' },
  { id: 'past', label: 'Past & cancelled' },
]

export default function TicketsPage() {
  const queryClient = useQueryClient()
  const { role } = useAuth()
  const [activeTab, setActiveTab] = useState('all')
  const [pnr, setPnr] = useState('')
  const [selectedTicket, setSelectedTicket] = useState(null)
  const ticketsQuery = useQuery({
    queryKey: ['tickets', 'mine'],
    queryFn: ticketsApi.mine,
  })

  const pnrLookup = useMutation({
    mutationFn: ticketsApi.byPnr,
    onSuccess: (ticket) => setSelectedTicket(ticket),
    onError: (error) => toast.error(getErrorMessage(error, 'Ticket not found')),
  })

  const cancelTicket = useMutation({
    mutationFn: ticketsApi.cancel,
    onSuccess: () => {
      toast.success('Ticket cancelled successfully')
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      setSelectedTicket(null)
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to cancel ticket')),
  })

  const tickets = useMemo(() => ticketsQuery.data || [], [ticketsQuery.data])
  const filtered = useMemo(() => {
    if (activeTab === 'upcoming') {
      return tickets.filter((ticket) => ['BOOKED', 'PENDING_PAYMENT'].includes(ticket.status))
    }
    if (activeTab === 'payment') {
      return tickets.filter((ticket) => ticket.status === 'PENDING_PAYMENT')
    }
    if (activeTab === 'past') {
      return tickets.filter((ticket) => ['USED', 'CANCELLED', 'EXPIRED', 'PAYMENT_FAILED'].includes(ticket.status))
    }
    return tickets
  }, [tickets, activeTab])

  async function handleCancel(ticket) {
    const result = await confirmAction({
      title: 'Cancel this ticket?',
      text: `PNR ${ticket.pnrNumber} will no longer be valid for travel.`,
      confirmText: 'Cancel ticket',
      confirmColor: '#dc2626',
    })
    if (result.isConfirmed) cancelTicket.mutate(ticket.ticketId)
  }

  if (ticketsQuery.isLoading) return <PageLoader label="Collecting your digital tickets" />
  if (ticketsQuery.isError) {
    return <ErrorState error={ticketsQuery.error} onRetry={ticketsQuery.refetch} />
  }

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow={role === 'CONDUCTOR' ? 'Conductor ticketing' : 'Journey wallet'}
        title={role === 'CONDUCTOR' ? 'Issued tickets' : 'Your tickets'}
        description={
          role === 'CONDUCTOR'
            ? 'Review walk-up tickets issued through your conductor account and find them by PNR.'
            : 'Keep every booking, payment status and journey detail in one place.'
        }
        actions={
          <Link className="button button--primary" to="/routes">
            <FiSearch /> {role === 'CONDUCTOR' ? 'Issue ticket' : 'Book a journey'}
          </Link>
        }
      />

      <section className="ticket-toolbar">
        <div className="tabs" role="tablist">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={activeTab === tab.id ? 'active' : ''}
              onClick={() => setActiveTab(tab.id)}
              role="tab"
              aria-selected={activeTab === tab.id}
            >
              {tab.label}
              {tab.id === 'payment' && (
                <span>{tickets.filter((ticket) => ticket.status === 'PENDING_PAYMENT').length}</span>
              )}
            </button>
          ))}
        </div>
        <form
          className="compact-search"
          onSubmit={(event) => {
            event.preventDefault()
            if (!pnr.trim()) return
            pnrLookup.mutate(pnr.trim())
          }}
        >
          <FiSearch />
          <input
            value={pnr}
            onChange={(event) => setPnr(event.target.value.toUpperCase())}
            placeholder="Find by PNR"
            aria-label="Find ticket by PNR"
          />
          <button disabled={pnrLookup.isPending}>Find</button>
        </form>
      </section>

      {filtered.length ? (
        <section className="ticket-grid">
          {filtered.map((ticket) => (
            <article className="ticket-card" key={ticket.ticketId}>
              <header className="ticket-card__header">
                <div>
                  <span>PNR</span>
                  <strong>{ticket.pnrNumber}</strong>
                </div>
                <StatusBadge status={ticket.status} />
              </header>
              <div className="ticket-card__route">
                <div>
                  <span />
                  <strong>Stop #{ticket.sourceStopId}</strong>
                  <small>Boarding</small>
                </div>
                <i>
                  <span />
                  <FiTag />
                  <span />
                </i>
                <div>
                  <span />
                  <strong>Stop #{ticket.destinationStopId}</strong>
                  <small>Destination</small>
                </div>
              </div>
              <div className="ticket-card__details">
                <span><FiCalendar /><small>Date</small><strong>{formatDate(ticket.serviceDate)}</strong></span>
                <span><FiClock /><small>Departure</small><strong>{formatTime(ticket.departureTime)}</strong></span>
                <span><FiUsers /><small>Passengers</small><strong>{ticket.passengerCount}</strong></span>
              </div>
              <footer className="ticket-card__footer">
                <div>
                  <span>Total fare</span>
                  <strong>{formatCurrency(ticket.fareAmount)}</strong>
                </div>
                <div>
                  <button className="button button--ghost button--small" onClick={() => setSelectedTicket(ticket)}>
                    View details
                  </button>
                  {ticket.status === 'PENDING_PAYMENT' && (
                    <Link
                      className="button button--primary button--small"
                      to={`/payments?ticketId=${ticket.ticketId}&amount=${ticket.fareAmount}`}
                    >
                      <FiCreditCard /> Pay
                    </Link>
                  )}
                </div>
              </footer>
            </article>
          ))}
        </section>
      ) : (
        <section className="panel">
          <EmptyState
            icon={FiTag}
            title={activeTab === 'all' ? 'No tickets yet' : 'No tickets in this category'}
            description="Your bookings will appear here as soon as you reserve a journey."
            action={
              <Link className="button button--primary" to="/routes">
                Browse routes
              </Link>
            }
          />
        </section>
      )}

      <Modal
        open={Boolean(selectedTicket)}
        onClose={() => setSelectedTicket(null)}
        title={`Ticket ${selectedTicket?.pnrNumber || ''}`}
        description="Secure digital journey record"
      >
        {selectedTicket && (
          <div className="ticket-detail">
            <div className="ticket-detail__banner">
              <span className="route-code route-code--large">R{selectedTicket.routeId}</span>
              <div>
                <strong>Route #{selectedTicket.routeId}</strong>
                <span>Schedule #{selectedTicket.scheduleId}</span>
              </div>
              <StatusBadge status={selectedTicket.status} />
            </div>
            <dl>
              <div><dt>Service date</dt><dd>{formatDate(selectedTicket.serviceDate)}</dd></div>
              <div><dt>Departure</dt><dd>{formatTime(selectedTicket.departureTime)}</dd></div>
              <div><dt>Passengers</dt><dd>{selectedTicket.passengerCount}</dd></div>
              <div><dt>Fare</dt><dd>{formatCurrency(selectedTicket.fareAmount)}</dd></div>
              <div><dt>Vehicle</dt><dd>{selectedTicket.vehicleId ? `#${selectedTicket.vehicleId}` : 'Assigned later'}</dd></div>
              <div><dt>Booked at</dt><dd>{formatDate(selectedTicket.bookedAt, 'dd MMM yyyy, hh:mm a')}</dd></div>
            </dl>
            <div className="modal__actions">
              {selectedTicket.status === 'PENDING_PAYMENT' && (
                <Link
                  className="button button--primary"
                  to={`/payments?ticketId=${selectedTicket.ticketId}&amount=${selectedTicket.fareAmount}`}
                  onClick={() => setSelectedTicket(null)}
                >
                  <FiCreditCard /> Complete payment
                </Link>
              )}
              {['PENDING_PAYMENT', 'BOOKED'].includes(selectedTicket.status) && (
                <button
                  className="button button--danger"
                  onClick={() => handleCancel(selectedTicket)}
                  disabled={cancelTicket.isPending}
                >
                  <FiTrash2 /> Cancel ticket
                </button>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
