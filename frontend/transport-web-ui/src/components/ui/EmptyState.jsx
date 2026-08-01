import { FiInbox } from 'react-icons/fi'

export default function EmptyState({
  icon: Icon = FiInbox,
  title = 'Nothing here yet',
  description = 'New information will appear here when it becomes available.',
  action,
}) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon">
        <Icon />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  )
}
