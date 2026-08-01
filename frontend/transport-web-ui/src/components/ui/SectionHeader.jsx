export default function SectionHeader({ eyebrow, title, description, actions }) {
  return (
    <header className="section-header">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h2>{title}</h2>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="section-header__actions">{actions}</div>}
    </header>
  )
}
