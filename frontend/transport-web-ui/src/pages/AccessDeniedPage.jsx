import { Link } from 'react-router-dom'
import { FiArrowLeft, FiShield } from 'react-icons/fi'
import Brand from '../components/ui/Brand.jsx'

export default function AccessDeniedPage() {
  return (
    <main className="system-page">
      <Brand />
      <div className="system-page__card">
        <span className="system-page__icon"><FiShield /></span>
        <span className="eyebrow">Access restricted</span>
        <h1>This workspace belongs to another role.</h1>
        <p>Your account is secure, but it does not have permission to open this page.</p>
        <Link className="button button--primary" to="/dashboard">
          <FiArrowLeft /> Return to dashboard
        </Link>
      </div>
    </main>
  )
}
