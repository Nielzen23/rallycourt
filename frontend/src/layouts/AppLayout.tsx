import { useState } from 'react'
import { Building2, CircleUserRound, Home, LogOut, Menu, X } from 'lucide-react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import HeaderLogo from '../components/HeaderLogo'
import {
  headerIconButtonActiveClassName,
  headerIconButtonClassName,
} from '../components/headerStyles'
import {
  clearAuthSession,
  getStoredEmail,
  getStoredFirstName,
  getStoredLastName,
  getStoredMobileNumber,
  getStoredRole,
  setStoredEmail,
  setStoredFirstName,
  setStoredLastName,
  setStoredMobileNumber,
} from '../utils/authStorage'

const profileInputClassName =
  'min-h-[42px] w-full rounded-xl border border-slate-300 bg-white px-3.5 text-slate-900 outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100'

function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const [isUserModalOpen, setIsUserModalOpen] = useState(false)
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [profile, setProfile] = useState(() => ({
    firstName: getStoredFirstName() ?? '',
    lastName: getStoredLastName() ?? '',
    email: getStoredEmail() ?? '',
    mobileNumber: getStoredMobileNumber() ?? '',
  }))
  const [draftProfile, setDraftProfile] = useState(profile)
  const displayName = profile.firstName || profile.email
  const role = getStoredRole()
  const isAdmin = role === 'ADMIN'
  const activeSection =
    location.pathname === '/court-management' ? 'Court Management' : 'Dashboard'

  const handleSignOut = () => {
    clearAuthSession()
    navigate('/')
    setIsMenuOpen(false)
  }

  const openUserModal = () => {
    setDraftProfile(profile)
    setIsUserModalOpen(true)
  }

  const handleSaveProfile = () => {
    const nextProfile = {
      ...draftProfile,
      firstName: draftProfile.firstName.trim(),
      lastName: draftProfile.lastName.trim(),
      email: draftProfile.email.trim(),
      mobileNumber: draftProfile.mobileNumber.trim(),
    }

    setStoredFirstName(nextProfile.firstName)
    setStoredLastName(nextProfile.lastName)
    setStoredEmail(nextProfile.email)
    setStoredMobileNumber(nextProfile.mobileNumber)
    setProfile((current) => ({
      ...current,
      ...nextProfile,
    }))
    setIsUserModalOpen(false)
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur-xl">
        <div className="mx-auto flex min-h-[72px] max-w-[1280px] flex-col gap-3 px-[18px] py-[14px] sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:gap-8 lg:py-0">
          <Link className="inline-flex items-center gap-3 text-lg font-bold text-slate-900 no-underline" to="/dashboard">
            <HeaderLogo alt="RallyCourt logo" className="sm:h-20 sm:w-20" />
          </Link>
          <button
            className={`${headerIconButtonClassName} lg:hidden`}
            type="button"
            aria-label={isMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            onClick={() => setIsMenuOpen((current) => !current)}
          >
            {isMenuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
          <div className="hidden w-full items-center justify-between gap-3 lg:flex lg:w-auto lg:justify-start">
            <nav className="flex items-center gap-2.5" aria-label="Application sections">
              <Link
                className={`inline-flex h-10 items-center justify-center gap-2 rounded-xl border px-3 text-sm font-medium transition ${
                  location.pathname === '/dashboard'
                    ? headerIconButtonActiveClassName
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:text-slate-900'
                }`}
                to="/dashboard"
                aria-label="Home tab"
                title="Home"
              >
                <Home size={18} />
                <span>Dashboard</span>
              </Link>
              {isAdmin ? (
                <Link
                  className={`${headerIconButtonClassName} ${
                    location.pathname === '/court-management'
                      ? headerIconButtonActiveClassName
                      : ''
                  }`}
                  to="/court-management"
                  aria-label="Courts tab"
                  title="Courts"
                >
                  <Building2 size={18} />
                </Link>
              ) : null}
            </nav>
            {location.pathname === '/court-management' ? (
              <span className="text-right text-[0.95rem] font-semibold text-slate-900">{activeSection}</span>
            ) : null}
          </div>
          <div className="hidden w-full items-center justify-end gap-2.5 lg:flex lg:w-auto lg:min-w-[220px]">
            <button
              className="inline-flex items-center gap-2 rounded-xl bg-blue-50 px-3 py-2 text-blue-600 transition hover:bg-blue-100"
              type="button"
              aria-label="Open user profile"
              onClick={openUserModal}
            >
              <CircleUserRound size={18} />
              {displayName ? (
                <span className="max-w-[140px] truncate text-[0.92rem] font-medium text-slate-700" title={profile.email || displayName}>
                  {displayName}
                </span>
              ) : null}
            </button>
            <button
              className="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-red-600 text-white transition hover:bg-red-700 lg:h-10 lg:w-10"
              type="button"
              aria-label="Sign out"
              title="Sign out"
              onClick={handleSignOut}
            >
              <LogOut size={18} />
            </button>
          </div>
          {isMenuOpen ? (
            <div className="grid gap-4 border-t border-slate-200 pt-4 lg:hidden">
              <div className="flex items-center justify-between gap-3">
                <nav className="flex items-center gap-2.5" aria-label="Application sections">
                  <Link
                    className={`inline-flex h-10 items-center justify-center gap-2 rounded-xl border px-3 text-sm font-medium transition ${
                      location.pathname === '/dashboard'
                        ? headerIconButtonActiveClassName
                        : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:text-slate-900'
                    }`}
                    to="/dashboard"
                    aria-label="Home tab"
                    title="Home"
                    onClick={() => setIsMenuOpen(false)}
                  >
                    <Home size={18} />
                    <span>Dashboard</span>
                  </Link>
                  {isAdmin ? (
                    <Link
                      className={`${headerIconButtonClassName} ${
                        location.pathname === '/court-management'
                          ? headerIconButtonActiveClassName
                          : ''
                      }`}
                      to="/court-management"
                      aria-label="Courts tab"
                      title="Courts"
                      onClick={() => setIsMenuOpen(false)}
                    >
                      <Building2 size={18} />
                    </Link>
                  ) : null}
                </nav>
                {location.pathname === '/court-management' ? (
                  <span className="text-right text-[0.95rem] font-semibold text-slate-900">{activeSection}</span>
                ) : null}
              </div>
              <div className="flex items-center justify-between gap-2.5">
                <button
                  className="inline-flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl bg-blue-50 px-3 py-2 text-blue-600 transition hover:bg-blue-100"
                  type="button"
                  aria-label="Open user profile"
                  onClick={() => {
                    setIsMenuOpen(false)
                    openUserModal()
                  }}
                >
                  <CircleUserRound size={18} />
                  {displayName ? (
                    <span className="truncate text-[0.92rem] font-medium text-slate-700" title={profile.email || displayName}>
                      {displayName}
                    </span>
                  ) : null}
                </button>
                <button
                  className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-red-600 text-white transition hover:bg-red-700"
                  type="button"
                  aria-label="Sign out"
                  title="Sign out"
                  onClick={handleSignOut}
                >
                  <LogOut size={18} />
                </button>
              </div>
            </div>
          ) : null}
        </div>
      </header>
      {isUserModalOpen ? (
        <div
          className="fixed inset-0 z-80 flex items-center justify-center bg-slate-900/48 p-3 backdrop-blur-sm sm:p-6"
          role="presentation"
          onClick={() => setIsUserModalOpen(false)}
        >
          <section
            className="relative grid w-full max-w-[520px] gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_28px_72px_rgba(15,23,42,0.28)] sm:gap-5 sm:p-6"
            aria-label="User profile"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div className="grid gap-2">
                <span className="text-xs font-bold uppercase text-blue-600">Profile</span>
                <h2 className="text-2xl font-semibold text-slate-900">User information</h2>
                <p className="text-slate-500">Review and update your stored account details.</p>
              </div>
              <button
                className="inline-flex min-h-10 items-center justify-center rounded-xl bg-transparent px-3.5 text-slate-500 transition hover:text-slate-900"
                type="button"
                onClick={() => setIsUserModalOpen(false)}
              >
                Close
              </button>
            </div>
            <div className="grid gap-3.5">
              <label className="grid gap-2">
                <span className="text-sm font-semibold text-slate-900">First Name</span>
                <input
                  className={profileInputClassName}
                  value={draftProfile.firstName}
                  onChange={(event) =>
                    setDraftProfile((current) => ({
                      ...current,
                      firstName: event.target.value,
                    }))
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-sm font-semibold text-slate-900">Last Name</span>
                <input
                  className={profileInputClassName}
                  value={draftProfile.lastName}
                  onChange={(event) =>
                    setDraftProfile((current) => ({
                      ...current,
                      lastName: event.target.value,
                    }))
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-sm font-semibold text-slate-900">Email</span>
                <input
                  className={profileInputClassName}
                  type="email"
                  value={draftProfile.email}
                  onChange={(event) =>
                    setDraftProfile((current) => ({
                      ...current,
                      email: event.target.value,
                    }))
                  }
                />
              </label>
              <label className="grid gap-2">
                <span className="text-sm font-semibold text-slate-900">Mobile Number</span>
                <input
                  className={profileInputClassName}
                  type="tel"
                  value={draftProfile.mobileNumber}
                  onChange={(event) =>
                    setDraftProfile((current) => ({
                      ...current,
                      mobileNumber: event.target.value,
                    }))
                  }
                />
              </label>
            </div>
            <div className="flex flex-col gap-2.5 sm:flex-row sm:justify-end">
              <button
                className="inline-flex min-h-10 items-center justify-center rounded-xl bg-blue-50 px-4 text-sm font-semibold text-blue-600 transition hover:bg-blue-100"
                type="button"
                onClick={() => setIsUserModalOpen(false)}
              >
                Cancel
              </button>
              <button
                className="inline-flex min-h-10 items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700"
                type="button"
                onClick={handleSaveProfile}
              >
                Save Changes
              </button>
            </div>
          </section>
        </div>
      ) : null}
      <Outlet />
    </div>
  )
}

export default AppLayout
