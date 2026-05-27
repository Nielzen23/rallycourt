const steps = [
  'Choose a court',
  'Select date, time, and duration',
  'Confirm payment',
  'Play your game',
]

function HowItWorksSection() {
  return (
    <section className="grid gap-6 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <div className="grid gap-2">
        <h2 className="text-2xl font-semibold text-slate-900">How It Works</h2>
        <p className="text-sm leading-7 text-slate-600">
          The reservation flow stays short and predictable.
        </p>
      </div>
      <ol className="grid gap-4 lg:grid-cols-4">
        {steps.map((step, index) => (
          <li
            key={step}
            className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-5"
          >
            <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-teal-700 text-sm font-semibold text-white">
              {index + 1}
            </span>
            <span className="text-sm font-medium text-slate-800">{step}</span>
          </li>
        ))}
      </ol>
    </section>
  )
}

export default HowItWorksSection
