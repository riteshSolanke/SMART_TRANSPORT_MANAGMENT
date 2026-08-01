import { FiAlertTriangle, FiRefreshCw } from 'react-icons/fi'
import { getErrorMessage } from '../../lib/apiClient.js'

export default function ErrorState({ error, onRetry, compact = false }) {
  return (
    <div className={`error-state ${compact ? 'error-state--compact' : ''}`} role="alert">
      <FiAlertTriangle aria-hidden="true" />
      <div>
        <strong>We couldn’t load this information</strong>
        <p>{getErrorMessage(error)}</p>
      </div>
      {onRetry && (
        <button className="button button--secondary button--small" onClick={onRetry}>
          <FiRefreshCw />
          Try again
        </button>
      )}
    </div>
  )
}
