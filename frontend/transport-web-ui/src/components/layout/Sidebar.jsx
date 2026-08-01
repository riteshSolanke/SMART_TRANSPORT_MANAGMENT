import { NavLink } from 'react-router-dom'
import { FiChevronLeft, FiChevronRight, FiHelpCircle, FiShield } from 'react-icons/fi'
import Brand from '../ui/Brand.jsx'
import { navigationForRole } from '../../config/navigation.jsx'
import { useAuth } from '../../context/authContext.js'
import { humanize } from '../../lib/formatters.js'

export default function Sidebar({
  collapsed,
  mobileOpen,
  onCloseMobile,
  onToggleCollapse,
}) {
  const { role } = useAuth()
  const items = navigationForRole(role)

  return (
    <aside
      className={`sidebar ${collapsed ? 'sidebar--collapsed' : ''} ${
        mobileOpen ? 'sidebar--mobile-open' : ''
      }`}
    >
      <div className="sidebar__brand">
        <Brand compact={collapsed} light />
        <button
          className="sidebar__collapse"
          aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
          onClick={onToggleCollapse}
        >
          {collapsed ? <FiChevronRight /> : <FiChevronLeft />}
        </button>
      </div>

      <div className="sidebar__role">
        <span>
          <FiShield />
        </span>
        {!collapsed && (
          <div>
            <small>Signed in as</small>
            <strong>{humanize(role)}</strong>
          </div>
        )}
      </div>

      <nav className="sidebar__nav" aria-label="Main navigation">
        <span className="sidebar__section-label">{collapsed ? '•••' : 'Workspace'}</span>
        {items.map(({ label, path, icon: Icon }) => (
          <NavLink
            key={path}
            to={path}
            onClick={onCloseMobile}
            className={({ isActive }) =>
              `sidebar__link ${isActive ? 'sidebar__link--active' : ''}`
            }
            title={collapsed ? label : undefined}
          >
            <Icon aria-hidden="true" />
            {!collapsed && <span>{label}</span>}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar__footer">
        <div className="sidebar__help">
          <FiHelpCircle />
          {!collapsed && (
            <div>
              <strong>Need assistance?</strong>
              <span>Contact transport support</span>
            </div>
          )}
        </div>
        {!collapsed && <small>TransitFlow v1.0</small>}
      </div>
    </aside>
  )
}
