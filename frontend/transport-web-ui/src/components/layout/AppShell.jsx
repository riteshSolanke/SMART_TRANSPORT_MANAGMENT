import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'

export default function AppShell() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem('transitflow.sidebar') === 'collapsed',
  )

  useEffect(() => {
    localStorage.setItem('transitflow.sidebar', collapsed ? 'collapsed' : 'expanded')
  }, [collapsed])

  return (
    <div className={`app-shell ${collapsed ? 'app-shell--collapsed' : ''}`}>
      <Sidebar
        collapsed={collapsed}
        mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)}
        onToggleCollapse={() => setCollapsed((value) => !value)}
      />
      {mobileOpen && (
        <button
          className="sidebar-backdrop"
          aria-label="Close navigation"
          onClick={() => setMobileOpen(false)}
        />
      )}
      <div className="app-shell__body">
        <Topbar onOpenMenu={() => setMobileOpen(true)} />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
