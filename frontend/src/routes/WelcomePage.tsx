import { Link } from 'react-router-dom'
import heroImg from '../assets/hero.png'
import './WelcomePage.css'

function WelcomePage() {
  return (
    <main className="welcome-shell">
      <section className="welcome-hero">
        <div className="welcome-hero__media">
          <img src={heroImg} alt="RallyCourt venue preview" />
        </div>
        <div className="welcome-hero__copy">
          <span className="welcome-hero__eyebrow">RallyCourt</span>
          <h1>One queue for booking, payments, and court operations.</h1>
          <p>
            Start with the path that matches your role. Players can browse
            availability, staff can sign in, and future owner onboarding can
            branch from the same public entry screen.
          </p>
          <div className="welcome-hero__actions">
            <Link className="welcome-button welcome-button--primary" to="/login">
              Login
            </Link>
            <Link className="welcome-button" to="/register">
              Sign Up
            </Link>
            <Link className="welcome-button" to="/login">
              Continue Browsing
            </Link>
          </div>
        </div>
      </section>
    </main>
  )
}

export default WelcomePage
