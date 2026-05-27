type SessionExpiryModalProps = {
  remainingSeconds: number
  isRenewing: boolean
  errorMessage: string
  onRenew: () => void
  onSignOut: () => void
}

function SessionExpiryModal({
  remainingSeconds,
  isRenewing,
  errorMessage,
  onRenew,
  onSignOut,
}: SessionExpiryModalProps) {
  const minutes = Math.floor(remainingSeconds / 60)
  const seconds = remainingSeconds % 60
  const timeLabel = `${minutes}:${String(seconds).padStart(2, '0')}`

  return (
    <div className="fixed inset-0 z-[90] flex items-center justify-center bg-slate-900/52 p-4 backdrop-blur-sm">
      <section className="grid w-full max-w-md gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_28px_72px_rgba(15,23,42,0.28)]">
        <div className="grid gap-2">
          <span className="text-xs font-bold uppercase tracking-wide text-amber-600">Session</span>
          <h2 className="text-2xl font-semibold text-slate-900">Session expiring soon</h2>
          <p className="text-sm text-slate-600">
            Your session will expire in <span className="font-semibold text-slate-900">{timeLabel}</span>.
            Renew now to stay signed in.
          </p>
        </div>
        {errorMessage ? (
          <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {errorMessage}
          </div>
        ) : null}
        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
          <button
            className="inline-flex min-h-10 items-center justify-center rounded-xl bg-slate-100 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-200"
            type="button"
            onClick={onSignOut}
          >
            Sign Out
          </button>
          <button
            className="inline-flex min-h-10 items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
            type="button"
            onClick={onRenew}
            disabled={isRenewing}
          >
            {isRenewing ? 'Renewing...' : 'Renew Session'}
          </button>
        </div>
      </section>
    </div>
  )
}

export default SessionExpiryModal
