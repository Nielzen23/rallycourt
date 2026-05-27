import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import logo from '../../assets/RallyCourtLogo.png'
import LandingPage from '../../landing/pages/LandingPage'
import { useRegister } from '../hooks/useRegister'

const overlayClassName =
  'fixed inset-0 z-80 flex items-center justify-center bg-slate-900/48 p-3 backdrop-blur-sm sm:p-6'
const modalClassName =
  'relative grid w-full max-w-[520px] gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_28px_72px_rgba(15,23,42,0.28)] sm:gap-5 sm:p-6'
const closeButtonClassName =
  'absolute right-3 top-3 inline-flex h-10 w-10 items-center justify-center rounded-xl border border-transparent bg-transparent text-slate-500 transition hover:text-slate-900'
const brandRowClassName = 'mb-2 inline-flex items-center gap-3'
const fieldClassName = 'grid gap-2 text-left'
const inputClassName =
  'min-h-12 w-full rounded-xl border border-slate-300 bg-white px-3.5 text-slate-900 outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-100'
const primaryButtonClassName =
  'inline-flex min-h-12 items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-55'
const textLinkClassName =
  'border-0 bg-transparent p-0 font-semibold text-blue-600 transition hover:text-blue-700'
const feedbackBaseClassName = 'rounded-xl border px-3.5 py-3 text-left text-sm leading-6'

function RegisterPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [mobileNumber, setMobileNumber] = useState('')
  const [wantsCourtOwnerAccess, setWantsCourtOwnerAccess] = useState(false)
  const { isSubmitting, errorMessage, successMessage, submitRegister } =
    useRegister()

  const canSubmit = useMemo(() => {
    return (
      email.trim().length > 0 &&
      password.length >= 8 &&
      firstName.trim().length > 0 &&
      lastName.trim().length > 0 &&
      mobileNumber.trim().length > 0 &&
      !isSubmitting
    )
  }, [email, firstName, isSubmitting, lastName, mobileNumber, password.length])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!canSubmit) {
      return
    }

    const isSuccessful = await submitRegister({
      email,
      password,
      firstName,
      lastName,
      mobileNumber,
    }, wantsCourtOwnerAccess)

    if (isSuccessful) {
      window.setTimeout(() => {
        navigate('/login')
      }, 500)
    }
  }

  return (
    <>
      <LandingPage />
      <div className={overlayClassName} role="presentation" onClick={() => navigate('/')}>
        <section
          className={modalClassName}
          aria-label="Registration form"
          onClick={(event) => event.stopPropagation()}
        >
          <button
            className={closeButtonClassName}
            type="button"
            onClick={() => navigate('/')}
            aria-label="Close sign up form"
          >
            <X size={20} />
          </button>
          <div className="grid gap-2 text-left">
            <div className={brandRowClassName}>
              <img className="h-12 w-12 object-contain" src={logo} alt="RallyCourt logo" />
              <span className="text-[22px] font-bold leading-none text-slate-900">RallyCourt</span>
            </div>
            <h2 className="text-[34px] font-semibold leading-tight text-slate-900">Sign Up</h2>
            <p className="text-slate-500">Create a RallyCourt player account.</p>
          </div>

          <form className="grid gap-[18px]" onSubmit={handleSubmit}>
            <label className={fieldClassName}>
              <span className="text-sm font-semibold text-slate-900">Email Address</span>
              <input
                className={inputClassName}
                autoComplete="email"
                name="email"
                type="text"
                inputMode="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="Enter your email address"
              />
            </label>
            <label className={fieldClassName}>
              <span className="text-sm font-semibold text-slate-900">Password</span>
              <input
                className={inputClassName}
                autoComplete="new-password"
                name="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
              />
            </label>
            <label className={fieldClassName}>
              <span className="text-sm font-semibold text-slate-900">First Name</span>
              <input
                className={inputClassName}
                autoComplete="given-name"
                name="firstName"
                type="text"
                value={firstName}
                onChange={(event) => setFirstName(event.target.value)}
                placeholder="Enter your first name"
              />
            </label>
            <label className={fieldClassName}>
              <span className="text-sm font-semibold text-slate-900">Last Name</span>
              <input
                className={inputClassName}
                autoComplete="family-name"
                name="lastName"
                type="text"
                value={lastName}
                onChange={(event) => setLastName(event.target.value)}
                placeholder="Enter your last name"
              />
            </label>
            <label className={fieldClassName}>
              <span className="text-sm font-semibold text-slate-900">Mobile Number</span>
              <input
                className={inputClassName}
                autoComplete="tel"
                name="mobileNumber"
                type="tel"
                value={mobileNumber}
                onChange={(event) => setMobileNumber(event.target.value)}
                placeholder="09XXXXXXXXX"
              />
            </label>
            <label className="flex items-start gap-3 text-left text-slate-900">
              <input
                className="mt-[3px] h-4 w-4 shrink-0 accent-blue-600"
                checked={wantsCourtOwnerAccess}
                name="wantsCourtOwnerAccess"
                type="checkbox"
                onChange={(event) =>
                  setWantsCourtOwnerAccess(event.target.checked)
                }
              />
              <span className="grid gap-1 text-sm font-semibold leading-6">
                Register as court owner applicant
                <small className="text-[13px] font-normal leading-6 text-slate-500">
                  Your account will be created as a player first, then
                  submitted for admin approval.
                </small>
              </span>
            </label>

            {errorMessage ? (
              <p className={`${feedbackBaseClassName} border border-rose-200 bg-rose-50 text-rose-700`} role="alert">
                {errorMessage}
              </p>
            ) : null}

            {successMessage ? (
              <p className={`${feedbackBaseClassName} border border-emerald-200 bg-emerald-50 text-emerald-700`} role="status">
                {successMessage}
              </p>
            ) : null}

            <button className={primaryButtonClassName} type="submit" disabled={!canSubmit}>
              {isSubmitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <div className="grid gap-1.5 text-left text-[13px]">
            <p>
              <button className={textLinkClassName} type="button" onClick={() => navigate('/login')}>
                Already have an account? Login
              </button>
            </p>
          </div>
        </section>
      </div>
    </>
  )
}

export default RegisterPage
