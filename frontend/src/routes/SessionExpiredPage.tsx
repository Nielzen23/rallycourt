import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { clearAuthSession } from '../utils/authStorage'
import './SessionExpiredPage.css'

function SessionExpiredPage() {
  clearAuthSession()

  return (
    <main className="session-expired-shell">
      <section className="session-expired-panel app-card">
        <div className="session-expired-icon" aria-hidden="true">
          <ShieldAlert size={28} />
        </div>
        <div className="session-expired-copy">
          <span className="session-expired-eyebrow">Session</span>
          <h1>Session expired</h1>
          <p>Your sign-in session is no longer valid. Please sign in again to continue.</p>
        </div>
        <div className="session-expired-actions">
          <Link className="app-button" to="/login">
            Go to Login
          </Link>
          <Link className="app-button-secondary" to="/">
            Back to Home
          </Link>
        </div>
      </section>
    </main>
  )
}

export default SessionExpiredPage
