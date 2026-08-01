import clsx from 'clsx'
import { humanize } from '../../lib/formatters.js'

const positive = ['ACTIVE', 'AVAILABLE', 'SUCCESS', 'CONFIRMED', 'COMPLETED', 'BOOKED', 'USED']
const warning = ['PENDING', 'IN_SERVICE', 'PENDING_PAYMENT']
const negative = ['FAILED', 'CANCELLED', 'MAINTENANCE', 'PAYMENT_FAILED', 'EXPIRED']

export default function StatusBadge({ status, active }) {
  const resolved = typeof active === 'boolean' ? (active ? 'ACTIVE' : 'INACTIVE') : status
  const tone = positive.includes(resolved)
    ? 'success'
    : warning.includes(resolved)
      ? 'warning'
      : negative.includes(resolved) || resolved === 'INACTIVE'
        ? 'danger'
        : 'neutral'

  return (
    <span className={clsx('status-badge', `status-badge--${tone}`)}>
      <span aria-hidden="true" />
      {humanize(resolved)}
    </span>
  )
}
