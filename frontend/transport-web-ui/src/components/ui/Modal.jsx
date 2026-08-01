import { useEffect } from 'react'
import { FiX } from 'react-icons/fi'

export default function Modal({ open, title, description, onClose, children, size = 'medium' }) {
  useEffect(() => {
    if (!open) return undefined
    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.body.classList.add('modal-open')
    window.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.classList.remove('modal-open')
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="modal-layer" role="presentation" onMouseDown={onClose}>
      <section
        className={`modal modal--${size}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal__header">
          <div>
            <h2>{title}</h2>
            {description && <p>{description}</p>}
          </div>
          <button className="icon-button" aria-label="Close dialog" onClick={onClose}>
            <FiX />
          </button>
        </header>
        <div className="modal__body">{children}</div>
      </section>
    </div>
  )
}
