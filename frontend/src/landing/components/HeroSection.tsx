import { Link } from 'react-router-dom'
import heroImg from '../../assets/RallyCourtLogo.png'

function HeroSection() {
  return (
    <section className="border-b border-slate-200 bg-slate-50">
      <div className="mx-auto grid w-full max-w-7xl gap-10 px-6 py-14 lg:grid-cols-[minmax(0,1fr)_480px] lg:items-center lg:px-8 lg:py-20">
        <div className="grid gap-6">
          <span className="inline-flex w-fit rounded-full bg-teal-50 px-3 py-1 text-sm font-semibold text-teal-700">
            RallyCourt
          </span>
          <div className="grid gap-4">
            <h1 className="max-w-3xl text-5xl font-semibold tracking-tight text-slate-950 lg:text-6xl">
              Book your next court session with ease
            </h1>
            <p className="max-w-2xl text-lg leading-8 text-slate-600">
              Find courts, view map locations, reserve your preferred time
              slot, and confirm your booking in minutes.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
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
          </div>
        </div>

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <img
            alt="RallyCourt booking preview"
            className="h-full min-h-[320px] w-full object-cover"
            src={heroImg}
          />
        </div>
      </div>
    </section>
  )
}

export default HeroSection
