import clsx from 'clsx'
import { FiArrowUpRight } from 'react-icons/fi'

export default function StatCard({ label, value, helper, icon: Icon, tone = 'teal' }) {
  return (
    <article className="stat-card">
      <div className={clsx('stat-card__icon', `stat-card__icon--${tone}`)}>
        <Icon />
      </div>
      <div className="stat-card__content">
        <span>{label}</span>
        <strong>{value}</strong>
        {helper && (
          <small>
            <FiArrowUpRight aria-hidden="true" />
            {helper}
          </small>
        )}
      </div>
    </article>
  )
}
