import { FaBusSimple } from 'react-icons/fa6'

export default function Brand({ compact = false, light = false }) {
  return (
    <div className={`brand ${compact ? 'brand--compact' : ''} ${light ? 'brand--light' : ''}`}>
      <span className="brand__mark" aria-hidden="true">
        <FaBusSimple />
      </span>
      {!compact && (
        <span className="brand__copy">
          <strong>TransitFlow</strong>
          <small>Move smarter, every day</small>
        </span>
      )}
    </div>
  )
}
