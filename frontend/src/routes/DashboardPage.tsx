import { useEffect, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import { CircleDollarSign, Clock3, MapPinned } from 'lucide-react'
import { fetchDashboard } from '../dashboard/api/dashboardApi'
import type { DashboardCountItem, DashboardResponse } from '../dashboard/types/dashboardTypes'
import PageLayout from '../layouts/PageLayout'
import { fetchAllReservations, fetchMyReservations } from '../reservation/api/reservationApi'
import ReservationForm from '../reservation/components/ReservationForm'
import type { ReservationRecord } from '../reservation/types/reservationTypes'
import { processPayment } from '../payment/api/paymentApi'
import type { ApiErrorResponse } from '../types/api'
import { getStoredRole } from '../utils/authStorage'
import './DashboardPage.css'

function DashboardPage() {
  const reservationsPerPage = 5
  const role = getStoredRole()
  const [isReservationModalOpen, setIsReservationModalOpen] = useState(false)
  const [ongoingReservations, setOngoingReservations] = useState<ReservationRecord[]>([])
  const [isLoadingReservations, setIsLoadingReservations] = useState(true)
  const [reservationErrorMessage, setReservationErrorMessage] = useState('')
  const [selectedPaymentReservation, setSelectedPaymentReservation] = useState<ReservationRecord | null>(null)
  const [paymentMethod, setPaymentMethod] = useState('CARD')
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false)
  const [paymentStatusMessage, setPaymentStatusMessage] = useState('')
  const [ongoingReservationsPage, setOngoingReservationsPage] = useState(1)
  const [reservationHistoryPage, setReservationHistoryPage] = useState(1)
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [isLoadingDashboard, setIsLoadingDashboard] = useState(role === 'ADMIN')
  const [dashboardErrorMessage, setDashboardErrorMessage] = useState('')

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

  const reservationHistory = useMemo(() => {
    const now = Date.now()
    return ongoingReservations.filter((reservation) => {
      const endTime = new Date(reservation.endTime).getTime()
      return (
        reservation.status === 'CANCELLED' ||
        reservation.status === 'AUTO_CANCELLED' ||
        (!Number.isNaN(endTime) && endTime < now)
      )
    })
  }, [ongoingReservations, role])

  const paginatedVisibleReservations = useMemo(() => {
    const startIndex = (ongoingReservationsPage - 1) * reservationsPerPage
    return visibleReservations.slice(startIndex, startIndex + reservationsPerPage)
  }, [ongoingReservationsPage, reservationsPerPage, visibleReservations])

  const paginatedReservationHistory = useMemo(() => {
    const startIndex = (reservationHistoryPage - 1) * reservationsPerPage
    return reservationHistory.slice(startIndex, startIndex + reservationsPerPage)
  }, [reservationHistory, reservationHistoryPage, reservationsPerPage])

  const ongoingReservationsTotalPages = Math.max(1, Math.ceil(visibleReservations.length / reservationsPerPage))
  const reservationHistoryTotalPages = Math.max(1, Math.ceil(reservationHistory.length / reservationsPerPage))

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

  const formatPercentage = (value: number) => `${Math.round(value)}%`

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

  const loadDashboard = async () => {
    if (role !== 'ADMIN') {
      return null
    }

    setIsLoadingDashboard(true)
    setDashboardErrorMessage('')

    try {
      const response = await fetchDashboard()
      setDashboard(response)
      return response
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setDashboardErrorMessage(error.response?.data?.message ?? 'Unable to load dashboard.')
      } else {
        setDashboardErrorMessage('Unable to load dashboard.')
      }
      return null
    } finally {
      setIsLoadingDashboard(false)
    }
  }

  useEffect(() => {
    let active = true
    void (async () => {
      if (!active) {
        return
      }
      await loadReservations()
      if (role === 'ADMIN') {
        await loadDashboard()
      }
    })()
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    setOngoingReservationsPage((currentPage) => Math.min(currentPage, ongoingReservationsTotalPages))
  }, [ongoingReservationsTotalPages])

  useEffect(() => {
    setReservationHistoryPage((currentPage) => Math.min(currentPage, reservationHistoryTotalPages))
  }, [reservationHistoryTotalPages])

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
      await loadDashboard()
      setSelectedPaymentReservation(null)
    } catch {
      setPaymentStatusMessage('Payment failed. Try again.')
    } finally {
      setIsSubmittingPayment(false)
    }
  }

  const totalSportBookings = dashboard?.popularSports.reduce((sum, item) => sum + item.count, 0) ?? 0

  const renderPieChart = (
    title: string,
    items: DashboardCountItem[],
    total: number,
    emptyMessage: string,
    className?: string,
  ) => {
    if (items.length === 0 || total === 0) {
      return <p className="dashboard-card__feedback">{emptyMessage}</p>
    }

    const chartSegments = items.reduce<{ colorStops: string[]; legend: ReactNode[]; offset: number }>(
      (accumulator, item, index) => {
        const share = (item.count / total) * 100
        const endOffset = accumulator.offset + share
        const color = `var(--dashboard-chart-${(index % 6) + 1})`

        accumulator.colorStops.push(`${color} ${accumulator.offset}% ${endOffset}%`)
        accumulator.legend.push(
          <li key={item.label} className="dashboard-pie-legend__item">
            <span className="dashboard-pie-legend__label">
              <span className="dashboard-pie-legend__swatch" style={{ backgroundColor: color }} aria-hidden="true" />
              {item.label}
            </span>
            <span>{item.count} ({formatPercentage(share)})</span>
          </li>,
        )
        accumulator.offset = endOffset
        return accumulator
      },
      { colorStops: [], legend: [], offset: 0 },
    )

    return (
      <div className={`dashboard-pie ${className ?? ''}`.trim()}>
        <div className="dashboard-pie__visual">
          <div
            className="dashboard-pie__chart"
            role="img"
            aria-label={title}
            style={{ background: `conic-gradient(${chartSegments.colorStops.join(', ')})` }}
          />
          <div className="dashboard-pie__center">
            <strong>{total}</strong>
            <span>Total</span>
          </div>
        </div>
        <ul className="dashboard-pie-legend">{chartSegments.legend}</ul>
      </div>
    )
  }

  const renderReservationItems = (reservations: ReservationRecord[]) =>
    reservations.map((reservation) => (
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
          ) : reservation.status === 'CANCELLED' || reservation.status === 'AUTO_CANCELLED' ? (
            <span className="dashboard-status-badge dashboard-status-badge--cancelled">
              {reservation.status === 'AUTO_CANCELLED' ? 'Auto Cancelled' : 'Cancelled'}
            </span>
          ) : reservation.paymentStatus === 'SUCCESS' ? (
            <button
              className="dashboard-payment-button dashboard-payment-button--paid app-button-secondary"
              type="button"
              disabled
            >
              <CircleDollarSign size={14} /> Paid
            </button>
          ) : reservation.paymentStatus === 'FAILED' ? (
            <span className="dashboard-status-badge dashboard-status-badge--failed">Payment Failed</span>
          ) : reservation.paymentStatus === 'PENDING' ? (
            <span className="dashboard-status-badge dashboard-status-badge--pending">Payment Pending</span>
          ) : (
            <span className="dashboard-status-badge dashboard-status-badge--confirmed">Confirmed</span>
          )}
        </div>
        <dl>
          {role === 'ADMIN' ? (
            <>
              <div>
                <dt>Booked By</dt>
                <dd>{reservation.contactName ?? reservation.reservedBy}</dd>
              </div>
              <div>
                <dt>Mobile Number</dt>
                <dd>{reservation.contactMobileNumber ?? 'Not provided'}</dd>
              </div>
            </>
          ) : null}
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

  const renderReservationPagination = (
    currentPage: number,
    totalPages: number,
    onPageChange: (page: number) => void,
  ) => {
    if (totalPages <= 1) {
      return null
    }

    return (
      <div className="dashboard-pagination" aria-label="Reservation pagination">
        <span className="dashboard-pagination__summary">
          Page {currentPage} of {totalPages}
        </span>
        <div className="dashboard-pagination__actions">
          <button
            className="app-button-secondary dashboard-pagination__button"
            type="button"
            disabled={currentPage === 1}
            onClick={() => onPageChange(currentPage - 1)}
          >
            Previous
          </button>
          <button
            className="app-button-secondary dashboard-pagination__button"
            type="button"
            disabled={currentPage === totalPages}
            onClick={() => onPageChange(currentPage + 1)}
          >
            Next
          </button>
        </div>
      </div>
    )
  }

  return (
    <PageLayout
      title="Dashboard"
      description="Protected landing page after authentication. Use it as the navigation hub for court, reservation, and payment workflows."
    >
      <section className="dashboard-grid">
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
            {!isLoadingReservations && !reservationErrorMessage ? renderReservationItems(paginatedVisibleReservations) : null}
            {!isLoadingReservations && !reservationErrorMessage
              ? renderReservationPagination(
                  ongoingReservationsPage,
                  ongoingReservationsTotalPages,
                  setOngoingReservationsPage,
                )
              : null}
          </section>
        </div>
        <div className="dashboard-card dashboard-card--reservations app-card">
          <div className="dashboard-card__header">
            <strong>Reservation History</strong>
          </div>
          <span>Review bookings that have already finished.</span>
          <section className="dashboard-reservation-list" aria-label="Reservation history list">
            {isLoadingReservations ? <p className="dashboard-card__feedback">Loading reservation history...</p> : null}
            {!isLoadingReservations && reservationErrorMessage ? (
              <p className="dashboard-card__feedback dashboard-card__feedback--error">{reservationErrorMessage}</p>
            ) : null}
            {!isLoadingReservations && !reservationErrorMessage && reservationHistory.length === 0 ? (
              <p className="dashboard-card__feedback">No reservation history.</p>
            ) : null}
            {!isLoadingReservations && !reservationErrorMessage ? renderReservationItems(paginatedReservationHistory) : null}
            {!isLoadingReservations && !reservationErrorMessage
              ? renderReservationPagination(
                  reservationHistoryPage,
                  reservationHistoryTotalPages,
                  setReservationHistoryPage,
                )
              : null}
          </section>
        </div>
        {role === 'ADMIN' ? (
          <>
            <section className="dashboard-card dashboard-card--metrics app-card" aria-label="Dashboard booking summary">
              <strong>Booking Summary</strong>
              <span>Live read model for booking volume, payment completion, and user engagement.</span>
              {isLoadingDashboard ? (
                <p className="dashboard-card__feedback">Loading dashboard...</p>
              ) : null}
              {!isLoadingDashboard && dashboardErrorMessage ? (
                <p className="dashboard-card__feedback dashboard-card__feedback--error">{dashboardErrorMessage}</p>
              ) : null}
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard ? (
                <div className="dashboard-metrics-grid">
                  <article className="dashboard-metric-tile">
                    <span>Total bookings</span>
                    <strong>{dashboard.totalBookings}</strong>
                  </article>
                  <article className="dashboard-metric-tile">
                    <span>Paid bookings</span>
                    <strong>{dashboard.paidBookings}</strong>
                  </article>
                  <article className="dashboard-metric-tile">
                    <span>Users with bookings</span>
                    <strong>{dashboard.userSummary.usersWithBookings}</strong>
                  </article>
                  <article className="dashboard-metric-tile">
                    <span>Inactive users</span>
                    <strong>{dashboard.userSummary.inactiveUsers}</strong>
                  </article>
                </div>
              ) : null}
            </section>

            <section className="dashboard-card dashboard-card--chart app-card" aria-label="Booking payment breakdown">
              <strong>Bookings Paid vs Unpaid</strong>
              <span>Actual booking counts, not percentages.</span>
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard
                ? renderPieChart(
                    'Bookings paid vs unpaid',
                    dashboard.bookingPaymentBreakdown,
                    dashboard.totalBookings,
                    'No bookings available.',
                  )
                : null}
            </section>

            <section className="dashboard-card dashboard-card--chart app-card" aria-label="Popular sports">
              <strong>Most Booked Sports</strong>
              <span>Booking share by sport across all recorded reservations.</span>
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard
                ? renderPieChart(
                    'Most booked sports',
                    dashboard.popularSports,
                    totalSportBookings,
                    'No sport booking data available.',
                  )
                : null}
            </section>

            <section className="dashboard-card dashboard-card--list app-card" aria-label="Popular courts">
              <strong>Popular Courts</strong>
              <span>Top five courts ranked by booking count.</span>
              {isLoadingDashboard ? <p className="dashboard-card__feedback">Loading court rankings...</p> : null}
              {!isLoadingDashboard && dashboardErrorMessage ? (
                <p className="dashboard-card__feedback dashboard-card__feedback--error">{dashboardErrorMessage}</p>
              ) : null}
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard?.popularCourts.length === 0 ? (
                <p className="dashboard-card__feedback">No court booking data available.</p>
              ) : null}
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard?.popularCourts.length ? (
                <ol className="dashboard-ranking-list">
                  {dashboard.popularCourts.map((court) => (
                    <li key={court.label} className="dashboard-ranking-list__item">
                      <span>{court.label}</span>
                      <strong>{court.count}</strong>
                    </li>
                  ))}
                </ol>
              ) : null}
            </section>

            <section className="dashboard-card dashboard-card--chart app-card" aria-label="User activity summary">
              <strong>User Activity</strong>
              <span>Users split by bookings, active without bookings, and inactive usage.</span>
              {!isLoadingDashboard && !dashboardErrorMessage && dashboard
                ? renderPieChart(
                    'User activity summary',
                    [
                      {
                        label: 'With bookings',
                        count: dashboard.userSummary.usersWithBookings,
                      },
                      {
                        label: 'Active without bookings',
                        count: dashboard.userSummary.activeUsersWithoutBookings,
                      },
                      {
                        label: 'Inactive users',
                        count: dashboard.userSummary.inactiveUsers,
                      },
                    ],
                    dashboard.userSummary.usersWithBookings +
                      dashboard.userSummary.activeUsersWithoutBookings +
                      dashboard.userSummary.inactiveUsers,
                    'No user activity data available.',
                    'dashboard-pie--compact',
                  )
                : null}
            </section>

          </>
        ) : null}
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
                  contactName: matchedReservation?.contactName ?? null,
                  contactMobileNumber: matchedReservation?.contactMobileNumber ?? null,
                  startTime: reservation.startTime,
                  endTime: reservation.endTime,
                  expiresAt: reservation.expiresAt,
                  status: reservation.status,
                  paymentStatus: matchedReservation?.paymentStatus ?? null,
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
