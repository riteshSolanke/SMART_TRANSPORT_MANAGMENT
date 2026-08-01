import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { FiChevronDown, FiLogOut, FiMenu, FiUser } from 'react-icons/fi'
import toast from 'react-hot-toast'
import { useAuth } from '../../context/authContext.js'
import { confirmAction } from '../../lib/alerts.js'
import { humanize } from '../../lib/formatters.js'

const pageTitles = {
  '/dashboard': ['Command center', 'A clear view of today’s transport activity'],
  '/routes': ['Routes & schedules', 'Search services and manage the network'],
  '/book': ['Book a journey', 'Review your route, fare and passenger details'],
  '/tickets': ['Tickets', 'Manage upcoming and past journeys'],
  '/payments': ['Payments', 'Secure fare collection and transaction history'],
  '/fleet': ['Fleet operations', 'Vehicles, assignments and service readiness'],
  '/analytics': ['Service analytics', 'Usage, revenue and on-time performance'],
  '/users': ['User administration', 'Roles, staff accounts and access status'],
  '/profile': ['Account settings', 'Profile, verification and preferences'],
}

export default function Topbar({ onOpenMenu }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)
  const [title, subtitle] = pageTitles[pathname] || ['TransitFlow', 'Smart transport workspace']

  async function handleLogout() {
    setProfileOpen(false)
    const result = await confirmAction({
      title: 'Sign out of TransitFlow?',
      text: 'You will need to sign in again to access your workspace.',
      confirmText: 'Sign out',
      icon: 'question',
      confirmColor: '#0b2239',
    })
    if (!result.isConfirmed) return
    await logout()
    toast.success('You have been signed out safely')
    navigate('/login', { replace: true })
  }

  const initials = user?.name
    ? user.name
        .split(' ')
        .slice(0, 2)
        .map((part) => part[0])
        .join('')
        .toUpperCase()
    : 'TF'

  return (
    <header className="topbar">
      <div className="topbar__title">
        <button className="topbar__menu" aria-label="Open navigation" onClick={onOpenMenu}>
          <FiMenu />
        </button>
        <div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </div>

      <div className="topbar__actions">
        <span className="secure-pill">
          <span aria-hidden="true" />
          Secure gateway
        </span>
        <div className="profile-menu">
          <button
            className="profile-menu__trigger"
            aria-expanded={profileOpen}
            onClick={() => setProfileOpen((value) => !value)}
          >
            <span className="avatar">{initials}</span>
            <span className="profile-menu__copy">
              <strong>{user?.name || 'Transport user'}</strong>
              <small>{humanize(user?.role)}</small>
            </span>
            <FiChevronDown />
          </button>

          {profileOpen && (
            <div className="profile-menu__panel">
              <div className="profile-menu__summary">
                <span className="avatar avatar--large">{initials}</span>
                <div>
                  <strong>{user?.name || 'Transport user'}</strong>
                  <span>{user?.email || user?.mobileNumber}</span>
                </div>
              </div>
              <Link to="/profile" onClick={() => setProfileOpen(false)}>
                <FiUser />
                Account settings
              </Link>
              <button onClick={handleLogout}>
                <FiLogOut />
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
