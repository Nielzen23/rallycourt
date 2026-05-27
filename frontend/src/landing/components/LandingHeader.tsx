import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import HeaderLogo from '../../components/HeaderLogo'
import { headerIconButtonClassName } from '../../components/headerStyles'

function LandingHeader() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/95 backdrop-blur">
      <div className="mx-auto flex w-full max-w-7xl flex-col px-6 py-4 lg:px-8">
        <div className="flex items-center justify-between">
          <Link
            className="flex items-center text-slate-900"
            aria-label="RallyCourt home"
            to="/"
          >
            <HeaderLogo alt="RallyCourt logo" />
          </Link>
          <button
            className={`${headerIconButtonClassName} lg:hidden`}
            type="button"
            aria-label={isMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            onClick={() => setIsMenuOpen((current) => !current)}
          >
            {isMenuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
          <nav className="hidden items-center gap-3 text-sm font-medium lg:flex">
            <Link
              className="inline-flex min-h-10 items-center justify-center rounded-md bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-800"
              to="/login"
            >
              Login
            </Link>
            <Link
              className="inline-flex min-h-10 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
              to="/register"
            >
              Sign Up
            </Link>
          </nav>
        </div>
        {isMenuOpen ? (
          <nav className="mt-4 grid gap-3 text-sm font-medium lg:hidden">
            <Link
              className="inline-flex min-h-11 items-center justify-center rounded-md bg-teal-700 px-4 text-sm font-semibold text-white transition hover:bg-teal-800"
              to="/login"
              onClick={() => setIsMenuOpen(false)}
            >
              Login
            </Link>
            <Link
              className="inline-flex min-h-11 items-center justify-center rounded-md border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
              to="/register"
              onClick={() => setIsMenuOpen(false)}
            >
              Sign Up
            </Link>
          </nav>
        ) : null}
      </div>
    </header>
  )
}

export default LandingHeader
