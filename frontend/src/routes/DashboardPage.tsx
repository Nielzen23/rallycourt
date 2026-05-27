import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { CircleDollarSign, Clock3, MapPinned } from 'lucide-react'
import PageLayout from '../layouts/PageLayout'
import { fetchAllReservations, fetchMyReservations } from '../reservation/api/reservationApi'
import ReservationForm from '../reservation/components/ReservationForm'
import type { ReservationRecord } from '../reservation/types/reservationTypes'
import { processPayment } from '../payment/api/paymentApi'
import type { ApiErrorResponse } from '../types/api'
import { getStoredRole } from '../utils/authStorage'
import './DashboardPage.css'

function DashboardPage() {
  const role = getStoredRole()
  const canManageCourts = role !== 'PLAYER'
  const [isReservationModalOpen, setIsReservationModalOpen] = useState(false)
  const [ongoingReservations, setOngoingReservations] = useState<ReservationRecord[]>([])
  const [isLoadingReservations, setIsLoadingReservations] = useState(true)
  const [reservationErrorMessage, setReservationErrorMessage] = useState('')
  const [selectedPaymentReservation, setSelectedPaymentReservation] = useState<ReservationRecord | null>(null)
  const [paymentMethod, setPaymentMethod] = useState('CARD')
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false)
  const [paymentStatusMessage, setPaymentStatusMessage] = useState('')
  const courtManagementHref = '/court-management'
  const courtManagementDescription =
    'Manage court inventory, reservations, and payment visibility.'

  const visibleReservations = useMemo(() => {
    const now = Date.now()
    return ongoingReservations.filter((reservation) => {
      const endTime = new Date(reservation.endTime).getTime()
      return (
        (reservation.status === 'RESERVED_PENDING_PAYMENT' || reservation.status === 'CONFIRMED') &&
        !Number.isNaN(endTime) &&
        endTime >= now
      )
    })
  }, [ongoingReservations])

  const formatCity = (location: string | null) => {
    if (!location) {
      return 'Unknown location'
    }
    const segments = location.split(',').map((segment) => segment.trim()).filter(Boolean)
    return segments.length > 0 ? segments[segments.length - 1] : location
  }

  const formatDuration = (startTime: string, endTime: string) => {
    const durationMinutes = Math.max(
      0,
      Math.round((new Date(endTime).getTime() - new Date(startTime).getTime()) / 60000),
    )
    return `${durationMinutes} min`
  }

  const formatExpiration = (value: string | null) => {
    if (!value) {
      return 'This booking is held for 30 minutes.'
    }
    return `This booking is held for 30 minutes. Expires at ${new Date(value).toLocaleString()} local time.`
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat(undefined, { style: 'currency', currency: 'PHP' }).format(value)

  const openMapLocation = (reservation: ReservationRecord) => {
    if (reservation.latitude == null || reservation.longitude == null) {
      return
    }
    window.open(
      `https://www.google.com/maps?q=${reservation.latitude},${reservation.longitude}`,
      '_blank',
      'noopener,noreferrer',
    )
  }

  const loadReservations = async () => {
    setIsLoadingReservations(true)
    setReservationErrorMessage('')

    try {
      const response =
        role === 'ADMIN'
          ? await fetchAllReservations(0, 50)
          : await fetchMyReservations(0, 20)
      setOngoingReservations(response.content)
      return response.content
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setReservationErrorMessage(error.response?.data?.message ?? 'Unable to load ongoing reservations.')
      } else {
        setReservationErrorMessage('Unable to load ongoing reservations.')
      }
      return []
    } finally {
      setIsLoadingReservations(false)
    }
  }

  useEffect(() => {
    let active = true
    void (async () => {
      if (!active) {
        return
      }
      await loadReservations()
    })()
    return () => {
      active = false
    }
  }, [])

  const handlePaymentSubmit = async () => {
    if (!selectedPaymentReservation) {
      return
    }

    setIsSubmittingPayment(true)
    setPaymentStatusMessage('')

    try {
      await processPayment({
        reservationId: selectedPaymentReservation.id,
        amount: selectedPaymentReservation.amountDue,
      })
      setPaymentStatusMessage('Payment processed successfully.')
      await loadReservations()
      setSelectedPaymentReservation(null)
    } catch {
      setPaymentStatusMessage('Payment failed. Try again.')
    } finally {
      setIsSubmittingPayment(false)
    }
  }

  return (
    <PageLayout
      title="Dashboard"
      description="Protected landing page after authentication. Use it as the navigation hub for court, reservation, and payment workflows."
    >
      <section className="dashboard-grid">
        {canManageCourts ? (
          <Link className="dashboard-card app-card" to={courtManagementHref}>
            <strong>Court Management</strong>
            <span>{courtManagementDescription}</span>
          </Link>
        ) : null}
        <div className="dashboard-card dashboard-card--reservations app-card">
          <div className="dashboard-card__header">
            <strong>Ongoing Reservations</strong>
            <div className="dashboard-card__actions">
              <button
                className="dashboard-action-button app-button"
                type="button"
                onClick={() => setIsReservationModalOpen(true)}
              >
                Book Court
              </button>
            </div>
          </div>
          <span>Track active booking status and payment follow-through.</span>
          <section className="dashboard-reservation-list" aria-label="Reservations list">
            {isLoadingReservations ? <p className="dashboard-card__feedback">Loading ongoing reservations...</p> : null}
            {!isLoadingReservations && reservationErrorMessage ? (
              <p className="dashboard-card__feedback dashboard-card__feedback--error">{reservationErrorMessage}</p>
            ) : null}
            {!isLoadingReservations && !reservationErrorMessage && visibleReservations.length === 0 ? (
              <p className="dashboard-card__feedback">No ongoing reservations.</p>
            ) : null}
            {!isLoadingReservations && !reservationErrorMessage
              ? visibleReservations.map((reservation) => (
                  <article key={reservation.id} className="dashboard-reservation-item">
                    <header>
                      <h2>{reservation.courtName}</h2>
                    </header>
                    <div className="dashboard-reservation-item__meta">
                      <span>{reservation.courtType ?? 'Unknown sport'}</span>
                      <span>{formatCity(reservation.location)}</span>
                      <button
                        className="dashboard-map-button app-button-secondary"
                        type="button"
                        disabled={reservation.latitude == null || reservation.longitude == null}
                        onClick={() => openMapLocation(reservation)}
                      >
                        <MapPinned size={14} /> Map
                      </button>
                      {reservation.status === 'RESERVED_PENDING_PAYMENT' ? (
                        <button
                          className="dashboard-payment-button dashboard-payment-button--pending app-button-warning"
                          type="button"
                          onClick={() => {
                            setSelectedPaymentReservation(reservation)
                            setPaymentMethod('CARD')
                            setPaymentStatusMessage('')
                          }}
                        >
                          <Clock3 size={14} /> Pending Payment
                        </button>
                      ) : (
                        <button
                          className="dashboard-payment-button dashboard-payment-button--paid app-button-secondary"
                          type="button"
                          disabled
                        >
                          <CircleDollarSign size={14} /> Paid
                        </button>
                      )}
                    </div>
                    <dl>
                      <div>
                        <dt>When</dt>
                        <dd>{new Date(reservation.startTime).toLocaleString()}</dd>
                      </div>
                      <div>
                        <dt>How Long</dt>
                        <dd>{formatDuration(reservation.startTime, reservation.endTime)}</dd>
                      </div>
                    </dl>
                  </article>
                ))
              : null}
          </section>
        </div>
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
              onSubmit={async ({ reservation }) => {
                setIsReservationModalOpen(false)
                const reservations = await loadReservations()
                const matchedReservation = reservations.find((item) => item.id === reservation.id)
                setSelectedPaymentReservation({
                  id: reservation.id,
                  courtId: reservation.courtId,
                  courtName: matchedReservation?.courtName ?? `Court #${reservation.courtId}`,
                  courtType: matchedReservation?.courtType ?? null,
                  location: matchedReservation?.location ?? null,
                  latitude: matchedReservation?.latitude ?? null,
                  longitude: matchedReservation?.longitude ?? null,
                  reservedBy: reservation.reservedBy,
                  startTime: reservation.startTime,
                  endTime: reservation.endTime,
                  expiresAt: reservation.expiresAt,
                  status: reservation.status,
                  amountDue: reservation.amountDue,
                })
                setPaymentMethod('CARD')
                setPaymentStatusMessage('')
              }}
            />
          </section>
        </div>
      ) : null}
      {selectedPaymentReservation ? (
        <div
          className="page-form-modal-backdrop app-modal-backdrop"
          role="presentation"
          onClick={() => setSelectedPaymentReservation(null)}
        >
          <section
            className="page-form-modal app-modal app-modal-compact"
            aria-label="Payment form"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              className="page-form-modal-close page-form-modal-close--danger app-close-button"
              type="button"
              aria-label="Close payment form"
              onClick={() => setSelectedPaymentReservation(null)}
            >
              X
            </button>
            <div className="page-form-modal-header">
              <span className="page-form-modal-eyebrow app-eyebrow">Payment</span>
              <h2>Submit payment</h2>
              <p>Complete payment for {selectedPaymentReservation.courtName}.</p>
            </div>
            <p className="dashboard-payment-amount">
              Amount due: <strong>{formatCurrency(selectedPaymentReservation.amountDue)}</strong>
            </p>
            <p className="dashboard-payment-note" role="note">
              {formatExpiration(selectedPaymentReservation.expiresAt)}
            </p>
            <form
              className="dashboard-payment-form"
              onSubmit={async (event) => {
                event.preventDefault()
                await handlePaymentSubmit()
              }}
            >
              <fieldset>
                <legend>Payment method</legend>
                <label>
                  <input
                    checked={paymentMethod === 'CARD'}
                    name="payment-method"
                    type="radio"
                    onChange={() => setPaymentMethod('CARD')}
                  />
                  Card
                </label>
                <label>
                  <input
                    checked={paymentMethod === 'GCASH'}
                    name="payment-method"
                    type="radio"
                    onChange={() => setPaymentMethod('GCASH')}
                  />
                  GCash
                </label>
              </fieldset>
              {paymentStatusMessage ? (
                <p className="dashboard-card__feedback" role="status">
                  {paymentStatusMessage}
                </p>
              ) : null}
              <div className="dashboard-payment-actions">
                <button className="app-button" type="submit" disabled={isSubmittingPayment}>
                  {isSubmittingPayment ? 'Submitting payment...' : 'Pay now'}
                </button>
                <button
                  className="app-button-secondary"
                  type="button"
                  onClick={() => setSelectedPaymentReservation(null)}
                  disabled={isSubmittingPayment}
                >
                  Skip for now
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </PageLayout>
  )
}

export default DashboardPage
