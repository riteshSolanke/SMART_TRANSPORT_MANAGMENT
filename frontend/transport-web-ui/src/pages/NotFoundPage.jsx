import { Link } from 'react-router-dom'
import { FiArrowLeft, FiMapPin } from 'react-icons/fi'
import Brand from '../components/ui/Brand.jsx'

export default function NotFoundPage() {
  return (
    <main className="system-page">
      <Brand />
      <div className="system-page__card">
        <span className="system-page__number">404</span>
        <span className="system-page__icon"><FiMapPin /></span>
        <span className="eyebrow">Route not found</span>
        <h1>This stop is not on our network.</h1>
        <p>The page may have moved, or the address might be incomplete.</p>
        <Link className="button button--primary" to="/dashboard">
          <FiArrowLeft /> Return to TransitFlow
        </Link>
      </div>
    </main>
  )
}
