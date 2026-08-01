import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/authContext.js'

export default function ProtectedRoute({ allowedRoles }) {
  const { isAuthenticated, role } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    )
  }

  if (allowedRoles?.length && !allowedRoles.includes(role)) {
    return <Navigate to="/access-denied" replace />
  }

  return <Outlet />
}
