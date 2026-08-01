import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/layout/AppShell.jsx'
import PageLoader from './components/ui/PageLoader.jsx'
import { useAuth } from './context/authContext.js'
import ProtectedRoute from './routes/ProtectedRoute.jsx'

const AccessDeniedPage = lazy(() => import('./pages/AccessDeniedPage.jsx'))
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage.jsx'))
const BookingPage = lazy(() => import('./pages/BookingPage.jsx'))
const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const FleetPage = lazy(() => import('./pages/FleetPage.jsx'))
const ForgotPasswordPage = lazy(() => import('./pages/auth/ForgotPasswordPage.jsx'))
const LoginPage = lazy(() => import('./pages/auth/LoginPage.jsx'))
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage.jsx'))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage.jsx'))
const PaymentsPage = lazy(() => import('./pages/PaymentsPage.jsx'))
const ProfilePage = lazy(() => import('./pages/ProfilePage.jsx'))
const RoutesPage = lazy(() => import('./pages/RoutesPage.jsx'))
const TicketsPage = lazy(() => import('./pages/TicketsPage.jsx'))
const UsersPage = lazy(() => import('./pages/UsersPage.jsx'))

const managerRoles = ['TRANSPORT_MANAGER', 'ADMIN']
const fleetRoles = ['CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN']

function App() {
  const { isBootstrapping } = useAuth()

  if (isBootstrapping) {
    return <PageLoader fullScreen label="Preparing your transport workspace" />
  }

  return (
    <Suspense fallback={<PageLoader fullScreen label="Loading your workspace" />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/access-denied" element={<AccessDeniedPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/routes" element={<RoutesPage />} />
            <Route
              path="/book"
              element={<ProtectedRoute allowedRoles={['PASSENGER', 'ADMIN']} />}
            >
              <Route index element={<BookingPage />} />
            </Route>
            <Route
              path="/tickets"
              element={
                <ProtectedRoute allowedRoles={['PASSENGER', 'TRANSPORT_MANAGER', 'ADMIN']} />
              }
            >
              <Route index element={<TicketsPage />} />
            </Route>
            <Route
              path="/payments"
              element={<ProtectedRoute allowedRoles={['PASSENGER', 'ADMIN']} />}
            >
              <Route index element={<PaymentsPage />} />
            </Route>
            <Route
              path="/fleet"
              element={<ProtectedRoute allowedRoles={fleetRoles} />}
            >
              <Route index element={<FleetPage />} />
            </Route>
            <Route
              path="/analytics"
              element={<ProtectedRoute allowedRoles={managerRoles} />}
            >
              <Route index element={<AnalyticsPage />} />
            </Route>
            <Route
              path="/users"
              element={<ProtectedRoute allowedRoles={['ADMIN']} />}
            >
              <Route index element={<UsersPage />} />
            </Route>
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}

export default App
