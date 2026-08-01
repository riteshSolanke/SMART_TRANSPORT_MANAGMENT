import { FiBarChart2, FiClock, FiShield, FiSmartphone } from 'react-icons/fi'
import Brand from '../../components/ui/Brand.jsx'

export default function AuthLayout({ children, eyebrow, title, description }) {
  return (
    <div className="auth-page">
      <aside className="auth-story">
        <div className="auth-story__glow auth-story__glow--one" />
        <div className="auth-story__glow auth-story__glow--two" />
        <Brand light />
        <div className="auth-story__content">
          <span className="auth-story__eyebrow">One network. Every journey.</span>
          <h1>Public transport that moves at the speed of your city.</h1>
          <p>
            Search routes, book tickets, coordinate vehicles and understand service
            performance from one secure platform.
          </p>
          <div className="auth-story__features">
            <article>
              <FiSmartphone />
              <div>
                <strong>Digital ticketing</strong>
                <span>Book, pay and travel without queues.</span>
              </div>
            </article>
            <article>
              <FiClock />
              <div>
                <strong>Live operations</strong>
                <span>Keep schedules and fleet status in sync.</span>
              </div>
            </article>
            <article>
              <FiBarChart2 />
              <div>
                <strong>Actionable insight</strong>
                <span>Measure usage, revenue and punctuality.</span>
              </div>
            </article>
          </div>
        </div>
        <div className="auth-story__trust">
          <FiShield />
          <span>Protected by gateway authentication and role-based access</span>
        </div>
      </aside>
      <main className="auth-panel">
        <div className="auth-panel__mobile-brand">
          <Brand />
        </div>
        <div className="auth-card">
          <span className="eyebrow">{eyebrow}</span>
          <h2>{title}</h2>
          <p className="auth-card__description">{description}</p>
          {children}
        </div>
        <p className="auth-panel__footer">
          © {new Date().getFullYear()} TransitFlow · Smart Public Transport
        </p>
      </main>
    </div>
  )
}
