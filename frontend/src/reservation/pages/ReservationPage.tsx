import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import PageLayout from '../../layouts/PageLayout'
import type { ReservationSummary } from '../types/reservationTypes'
import ReservationForm from '../components/ReservationForm'
import './ReservationPage.css'

const reservationSamples: ReservationSummary[] = [
  {
    id: 1,
    courtName: 'Center Court',
    status: 'RESERVED_PENDING_PAYMENT',
    startTime: '2026-05-27 18:00',
    endTime: '2026-05-27 19:30',
  },
  {
    id: 2,
    courtName: 'North Court',
    status: 'CONFIRMED',
    startTime: '2026-05-28 08:00',
    endTime: '2026-05-28 09:00',
  },
]

function ReservationPage() {
  const [searchParams] = useSearchParams()
  const preselectedCourtId = searchParams.get('courtId') ?? ''
  const [isReservationModalOpen, setIsReservationModalOpen] = useState(false)

  useEffect(() => {
    if (preselectedCourtId) {
      setIsReservationModalOpen(true)
    }
  }, [preselectedCourtId])

  return (
    <PageLayout
      eyebrow="Reservation"
      title="Reservation Queue"
      description="Feature scaffold for reservation workflows, status visibility, and future payment follow-through."
    >
      <section className="reservation-toolbar">
        <button
          className="reservation-action app-button"
          type="button"
          onClick={() => setIsReservationModalOpen(true)}
        >
          Create reservation
        </button>
      </section>
      <section className="reservation-grid">
        {reservationSamples.map((reservation) => (
          <article key={reservation.id} className="reservation-card app-card">
            <header>
              <h2>{reservation.courtName}</h2>
              <span>{reservation.status}</span>
            </header>
            <dl>
              <div>
                <dt>Start</dt>
                <dd>{reservation.startTime}</dd>
              </div>
              <div>
                <dt>End</dt>
                <dd>{reservation.endTime}</dd>
              </div>
            </dl>
          </article>
        ))}
      </section>
      {isReservationModalOpen ? (
        <div
          className="page-form-modal-backdrop app-modal-backdrop"
          role="presentation"
          onClick={() => setIsReservationModalOpen(false)}
        >
          <section
            className="page-form-modal app-modal app-modal-compact"
            aria-label="Create reservation form"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              className="page-form-modal-close app-close-button"
              type="button"
              aria-label="Close reservation form"
              onClick={() => setIsReservationModalOpen(false)}
            >
              X
            </button>
            <div className="page-form-modal-header">
              <span className="page-form-modal-eyebrow app-eyebrow">Reservation</span>
              <h2>Create reservation</h2>
              <p>Select the court, start time, and duration for a new booking.</p>
            </div>
            <ReservationForm
              initialCourtId={preselectedCourtId}
              onSubmit={async () => {
                setIsReservationModalOpen(false)
              }}
            />
          </section>
        </div>
      ) : null}
    </PageLayout>
  )
}

export default ReservationPage
