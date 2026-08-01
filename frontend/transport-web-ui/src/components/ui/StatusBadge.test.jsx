import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.jsx'

describe('StatusBadge', () => {
  it('renders a readable success status', () => {
    render(<StatusBadge status="BOOKED" />)

    expect(screen.getByText('Booked')).toHaveClass(
      'status-badge',
      'status-badge--success',
    )
  })

  it('maps pending payment to the warning treatment', () => {
    render(<StatusBadge status="PENDING_PAYMENT" />)

    expect(screen.getByText('Pending Payment')).toHaveClass(
      'status-badge--warning',
    )
  })

  it('lets an explicit active flag determine the displayed state', () => {
    render(<StatusBadge status="BOOKED" active={false} />)

    expect(screen.getByText('Inactive')).toHaveClass('status-badge--danger')
  })
})
