import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  FiArrowRight,
  FiCreditCard,
  FiDollarSign,
  FiRefreshCcw,
  FiShield,
  FiSmartphone,
} from 'react-icons/fi'
import { RiWallet3Line } from 'react-icons/ri'
import toast from 'react-hot-toast'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { useAuth } from '../context/authContext.js'
import { confirmAction } from '../lib/alerts.js'
import { paymentsApi, ticketsApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { createIdempotencyKey, formatCurrency, formatDate } from '../lib/formatters.js'

const methods = [
  { id: 'UPI', label: 'UPI', detail: 'Fast mobile payment', icon: FiSmartphone },
  { id: 'CARD', label: 'Card', detail: 'Credit or debit card', icon: FiCreditCard },
  { id: 'WALLET', label: 'Wallet', detail: 'Digital transport wallet', icon: RiWallet3Line },
  { id: 'CASH', label: 'Cash', detail: 'Pay to conductor', icon: FiDollarSign },
]

export default function PaymentsPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { role } = useAuth()
  const ticketId = searchParams.get('ticketId')
  const [paymentMethod, setPaymentMethod] = useState('UPI')

  const paymentsQuery = useQuery({
    queryKey: ['payments', 'mine'],
    queryFn: paymentsApi.mine,
  })
  const ticketQuery = useQuery({
    queryKey: ['tickets', ticketId],
    queryFn: () => ticketsApi.get(ticketId),
    enabled: Boolean(ticketId),
  })

  const processPayment = useMutation({
    mutationFn: () =>
      paymentsApi.process(
        { ticketId: Number(ticketId), paymentMethod },
        createIdempotencyKey('payment'),
      ),
    onSuccess: (payment) => {
      toast.success(
        payment.status === 'SUCCESS'
          ? 'Payment completed and ticket confirmed'
          : `Payment status: ${payment.status}`,
      )
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      navigate('/tickets', { replace: true })
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Payment could not be completed')),
  })

  const refundPayment = useMutation({
    mutationFn: paymentsApi.refund,
    onSuccess: () => {
      toast.success('Payment refunded successfully')
      queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to refund payment')),
  })

  async function requestRefund(payment) {
    const result = await confirmAction({
      title: 'Refund this payment?',
      text: `${formatCurrency(payment.amount)} will be marked as refunded.`,
      confirmText: 'Issue refund',
      confirmColor: '#dc2626',
    })
    if (result.isConfirmed) refundPayment.mutate(payment.paymentId)
  }

  if (paymentsQuery.isLoading) return <PageLoader label="Loading secure payment history" />
  if (paymentsQuery.isError) {
    return <ErrorState error={paymentsQuery.error} onRetry={paymentsQuery.refetch} />
  }

  const payments = paymentsQuery.data || []
  const ticket = ticketQuery.data

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Fare collection"
        title={ticketId ? 'Complete payment' : 'Payment history'}
        description={
          ticketId
            ? 'Choose a payment method to confirm your reserved ticket.'
            : 'Review successful, failed and refunded fare transactions.'
        }
        actions={
          ticketId && (
            <Link className="button button--ghost" to="/tickets">
              Back to tickets
            </Link>
          )
        }
      />

      {ticketId && (
        <section className="payment-layout">
          <div className="panel payment-methods">
            <div className="panel__header panel__header--bordered">
              <div>
                <span className="eyebrow">Payment method</span>
                <h3>How would you like to pay?</h3>
              </div>
              <span className="secure-pill"><FiShield /> Encrypted</span>
            </div>
            {ticketQuery.isLoading ? (
              <div className="form-skeleton form-skeleton--large" />
            ) : ticketQuery.isError ? (
              <ErrorState error={ticketQuery.error} onRetry={ticketQuery.refetch} compact />
            ) : (
              <>
                <div className="payment-method-grid">
                  {methods.map(({ id, label, detail, icon: Icon }) => (
                    <label
                      className={`payment-method ${paymentMethod === id ? 'active' : ''}`}
                      key={id}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value={id}
                        checked={paymentMethod === id}
                        onChange={() => setPaymentMethod(id)}
                      />
                      <span><Icon /></span>
                      <div><strong>{label}</strong><small>{detail}</small></div>
                    </label>
                  ))}
                </div>
                <div className="payment-security-note">
                  <FiShield />
                  <div>
                    <strong>Secure fare processing</strong>
                    <span>Your request uses an idempotency key to prevent duplicate charges.</span>
                  </div>
                </div>
              </>
            )}
          </div>

          <aside className="panel checkout-summary">
            <span className="eyebrow">Order summary</span>
            <h3>Ticket #{ticketId}</h3>
            <dl>
              <div><dt>PNR</dt><dd>{ticket?.pnrNumber || '—'}</dd></div>
              <div><dt>Route</dt><dd>Route #{ticket?.routeId || '—'}</dd></div>
              <div><dt>Passengers</dt><dd>{ticket?.passengerCount || '—'}</dd></div>
              <div><dt>Service date</dt><dd>{formatDate(ticket?.serviceDate)}</dd></div>
            </dl>
            <div className="checkout-summary__total">
              <span>Amount due</span>
              <strong>{formatCurrency(ticket?.fareAmount || searchParams.get('amount'))}</strong>
            </div>
            <button
              className="button button--primary button--block"
              disabled={
                processPayment.isPending ||
                ticketQuery.isLoading ||
                ticket?.status !== 'PENDING_PAYMENT'
              }
              onClick={() => processPayment.mutate()}
            >
              {processPayment.isPending ? <span className="button-spinner" /> : <FiShield />}
              Pay securely
              {!processPayment.isPending && <FiArrowRight />}
            </button>
            {ticket && ticket.status !== 'PENDING_PAYMENT' && (
              <p className="field__error">This ticket is not awaiting payment.</p>
            )}
          </aside>
        </section>
      )}

      <section className="panel">
        <div className="panel__header panel__header--bordered">
          <div>
            <span className="eyebrow">Transactions</span>
            <h3>Recent payment activity</h3>
          </div>
          <span className="record-count">{payments.length} records</span>
        </div>
        {payments.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Transaction</th>
                  <th>Ticket</th>
                  <th>Method</th>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Status</th>
                  {role === 'ADMIN' && <th><span className="sr-only">Actions</span></th>}
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr key={payment.paymentId}>
                    <td>
                      <strong>{payment.transactionReference}</strong>
                      <small>#{payment.paymentId}</small>
                    </td>
                    <td>#{payment.ticketId}</td>
                    <td>{payment.paymentMethod}</td>
                    <td>{formatDate(payment.createdAt, 'dd MMM yyyy, hh:mm a')}</td>
                    <td><strong>{formatCurrency(payment.amount)}</strong></td>
                    <td><StatusBadge status={payment.status} /></td>
                    {role === 'ADMIN' && (
                      <td>
                        {payment.status === 'SUCCESS' && (
                          <button
                            className="button button--ghost button--small"
                            onClick={() => requestRefund(payment)}
                          >
                            <FiRefreshCcw /> Refund
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={FiCreditCard}
            title="No payment activity"
            description="Transactions will appear here after your first ticket payment."
          />
        )}
      </section>
    </div>
  )
}
