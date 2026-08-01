import { FaBusSimple } from 'react-icons/fa6'

export default function PageLoader({
  label = 'Loading the latest information',
  fullScreen = false,
}) {
  return (
    <div
      className={`page-loader ${fullScreen ? 'page-loader--fullscreen' : ''}`}
      role="status"
      aria-live="polite"
    >
      <div className="page-loader__track">
        <span className="page-loader__bus">
          <FaBusSimple />
        </span>
        <span className="page-loader__line" />
      </div>
      <strong>{label}</strong>
      <span>Please wait a moment</span>
    </div>
  )
}
