import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { CalendarClock, MapPin } from 'lucide-react'
import { fetchCourts } from '../../court/api/courtApi'
import type { Court } from '../../court/types/courtTypes'
import type { ApiErrorResponse } from '../../types/api'
import {
  createReservation,
  fetchCourtAvailability,
  fetchReservationConfig,
} from '../api/reservationApi'
import type { CourtAvailabilityItem, CreatedReservation } from '../types/reservationTypes'
import './ReservationForm.css'

type ReservationFormProps = {
  initialCourtId?: string
  onSubmit?: (result: {
    payload: {
      courtId: number
      startTime: string
      durationMinutes: number
    }
    reservation: CreatedReservation
  }) => void | Promise<void>
}

const courtTypeOptions = ['BASKETBALL', 'BADMINTON', 'PICKLEBALL', 'FUTSAL']
const venueTypeOptions = ['INDOOR', 'OUTDOOR']

function formatDateTimeLocal(value: Date) {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  const hours = `${value.getHours()}`.padStart(2, '0')
  const minutes = `${value.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function formatDateInput(value: Date) {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function addDays(base: Date, days: number) {
  const next = new Date(base)
  next.setDate(next.getDate() + days)
  return next
}

function roundUpToQuarterHour(base: Date) {
  const next = new Date(base)
  next.setSeconds(0, 0)
  const minutes = next.getMinutes()
  const roundedMinutes = Math.ceil(minutes / 15) * 15
  if (roundedMinutes === 60) {
    next.setHours(next.getHours() + 1, 0, 0, 0)
    return next
  }
  next.setMinutes(roundedMinutes, 0, 0)
  return next
}

function roundDownToQuarterHour(base: Date) {
  const next = new Date(base)
  next.setSeconds(0, 0)
  const minutes = next.getMinutes()
  next.setMinutes(Math.floor(minutes / 15) * 15, 0, 0)
  return next
}

function startOfDay(base: Date) {
  const next = new Date(base)
  next.setHours(0, 0, 0, 0)
  return next
}

function toApiDateTime(value: string) {
  return value.length === 16 ? `${value}:00` : value
}

function combineDateAndTime(date: string, time: string) {
  if (!date || !time) {
    return ''
  }
  return `${date}T${time}`
}

function buildQuarterHourOptions() {
  const options: string[] = []
  for (let hour = 0; hour < 24; hour += 1) {
    for (let minute = 0; minute < 60; minute += 15) {
      options.push(`${`${hour}`.padStart(2, '0')}:${`${minute}`.padStart(2, '0')}`)
    }
  }
  return options
}

function formatTimeLabel(value: string) {
  const [hourText, minuteText] = value.split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText)
  const suffix = hour >= 12 ? 'PM' : 'AM'
  const normalizedHour = hour % 12 === 0 ? 12 : hour % 12
  return `${normalizedHour}:${`${minute}`.padStart(2, '0')} ${suffix}`
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'PHP' }).format(value)
}

function calculateReservationAmount(hourlyRate: number, durationMinutes: number) {
  return (hourlyRate * durationMinutes) / 60
}

function getMinutesFromTimeText(value: string) {
  const [hourText, minuteText] = value.split(':')
  return Number(hourText) * 60 + Number(minuteText)
}

function overlaps(startIso: string, endIso: string, blockedSlots: CourtAvailabilityItem[]) {
  const requestedStart = new Date(startIso).getTime()
  const requestedEnd = new Date(endIso).getTime()

  return blockedSlots.some((slot) => {
    const blockedStart = new Date(slot.startDateTime).getTime()
    const blockedEnd = new Date(slot.endDateTime).getTime()
    return blockedStart < requestedEnd && blockedEnd > requestedStart
  })
}

function ReservationForm({ initialCourtId = '', onSubmit }: ReservationFormProps) {
  const [courtType, setCourtType] = useState('')
  const [venueType, setVenueType] = useState('')
  const [courtId, setCourtId] = useState(initialCourtId)
  const [startDate, setStartDate] = useState('')
  const [startClock, setStartClock] = useState('')
  const [durationMinutes, setDurationMinutes] = useState('')
  const [courts, setCourts] = useState<Court[]>([])
  const [availability, setAvailability] = useState<CourtAvailabilityItem[]>([])
  const [allowedDurations, setAllowedDurations] = useState<number[]>([60, 90, 120])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoadingCourts, setIsLoadingCourts] = useState(false)
  const [isLoadingAvailability, setIsLoadingAvailability] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const canLoadCourts = courtType.length > 0 && venueType.length > 0
  const now = useMemo(() => new Date(), [])
  const minimumReservationDate = useMemo(() => startOfDay(addDays(now, 1)), [now])
  const maximumReservationDate = useMemo(() => roundDownToQuarterHour(addDays(now, 14)), [now])
  const minDate = useMemo(() => formatDateInput(minimumReservationDate), [minimumReservationDate])
  const maxDate = useMemo(() => formatDateInput(maximumReservationDate), [maximumReservationDate])
  const timeOptions = useMemo(() => buildQuarterHourOptions(), [])
  const startTime = useMemo(() => combineDateAndTime(startDate, startClock), [startClock, startDate])

  const selectedCourt = useMemo(
    () => courts.find((court) => `${court.id}` === courtId) ?? null,
    [courtId, courts],
  )

  const computedAmountDue = useMemo(() => {
    if (!selectedCourt || !durationMinutes) {
      return null
    }

    return calculateReservationAmount(selectedCourt.hourlyRate, Number(durationMinutes))
  }, [durationMinutes, selectedCourt])

  const computedEndTime = useMemo(() => {
    if (!startTime || !durationMinutes) {
      return ''
    }

    const start = new Date(startTime)
    if (Number.isNaN(start.getTime())) {
      return ''
    }

    start.setMinutes(start.getMinutes() + Number(durationMinutes))
    return start.toLocaleString()
  }, [durationMinutes, startTime])

  const availableTimeOptions = useMemo(() => {
    if (!startDate) {
      return timeOptions
    }

    const selectedDuration = Number(durationMinutes || 0)
    const openMinutes = selectedCourt?.openTime ? getMinutesFromTimeText(selectedCourt.openTime) : 0
    const closeMinutes = selectedCourt?.closeTime ? getMinutesFromTimeText(selectedCourt.closeTime) : 24 * 60

    return timeOptions.filter((option) => {
      const candidate = new Date(`${startDate}T${option}:00`)
      const optionMinutes = getMinutesFromTimeText(option)
      const candidateEndMinutes = optionMinutes + selectedDuration
      const isWithinWindow = candidate >= minimumReservationDate && candidate <= maximumReservationDate
      const isWithinCourtHours =
        optionMinutes >= openMinutes &&
        (selectedDuration > 0 ? candidateEndMinutes <= closeMinutes : optionMinutes < closeMinutes)

      return isWithinWindow && isWithinCourtHours
    })
  }, [
    durationMinutes,
    maximumReservationDate,
    minimumReservationDate,
    selectedCourt?.closeTime,
    selectedCourt?.openTime,
    startDate,
    timeOptions,
  ])

  const hasConflict = useMemo(() => {
    if (!startTime || !durationMinutes) {
      return false
    }

    const end = new Date(startTime)
    if (Number.isNaN(end.getTime())) {
      return false
    }

    end.setMinutes(end.getMinutes() + Number(durationMinutes))
    return overlaps(startTime, end.toISOString(), availability)
  }, [availability, durationMinutes, startTime])

  useEffect(() => {
    let active = true

    const loadConfig = async () => {
      try {
        const response = await fetchReservationConfig()
        if (active) {
          setAllowedDurations(response.allowedDurationsMinutes)
        }
      } catch {
        if (active) {
          setAllowedDurations([60, 90, 120])
        }
      }
    }

    void loadConfig()
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!canLoadCourts) {
      setCourts([])
      setCourtId('')
      setAvailability([])
      return
    }

    let active = true
    setIsLoadingCourts(true)
    setErrorMessage('')

    const loadCourts = async () => {
      try {
        const response = await fetchCourts({
          courtType,
          venueType,
          status: 'AVAILABLE',
          page: 0,
          size: 20,
        })

        if (!active) {
          return
        }

        setCourts(response.content)
        if (!response.content.some((court) => `${court.id}` === courtId)) {
          setCourtId('')
          setAvailability([])
        }
      } catch (error) {
        if (!active) {
          return
        }

        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setErrorMessage(error.response?.data?.message ?? 'Unable to load available courts.')
        } else {
          setErrorMessage('Unable to load available courts.')
        }
        setCourts([])
      } finally {
        if (active) {
          setIsLoadingCourts(false)
        }
      }
    }

    void loadCourts()
    return () => {
      active = false
    }
  }, [canLoadCourts, courtId, courtType, venueType])

  useEffect(() => {
    if (!courtId || !startTime) {
      setAvailability([])
      return
    }

    let active = true
    const selectedDate = new Date(startTime)
    if (Number.isNaN(selectedDate.getTime())) {
      setAvailability([])
      return
    }

    const dayStart = new Date(selectedDate)
    dayStart.setHours(0, 0, 0, 0)
    const dayEnd = new Date(selectedDate)
    dayEnd.setHours(23, 59, 59, 0)

    setIsLoadingAvailability(true)
    setErrorMessage('')

    const loadAvailability = async () => {
      try {
        const response = await fetchCourtAvailability(
          Number(courtId),
          toApiDateTime(formatDateTimeLocal(dayStart)),
          toApiDateTime(formatDateTimeLocal(dayEnd)),
        )

        if (active) {
          setAvailability(response)
        }
      } catch (error) {
        if (!active) {
          return
        }

        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setErrorMessage(error.response?.data?.message ?? 'Unable to load court availability.')
        } else {
          setErrorMessage('Unable to load court availability.')
        }
        setAvailability([])
      } finally {
        if (active) {
          setIsLoadingAvailability(false)
        }
      }
    }

    void loadAvailability()
    return () => {
      active = false
    }
  }, [courtId, startTime])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!courtType || !venueType || !courtId || !startTime || !durationMinutes) {
      setErrorMessage('All reservation fields are required.')
      return
    }

    if (!allowedDurations.includes(Number(durationMinutes))) {
      setErrorMessage('Select a valid reservation duration.')
      return
    }

    if (hasConflict) {
      setErrorMessage('Selected time slot is no longer available.')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    const payload = {
      courtId: Number(courtId),
      startTime: toApiDateTime(startTime),
      durationMinutes: Number(durationMinutes),
    }

    try {
      const reservation = await createReservation(payload)
      setSuccessMessage('Reservation created.')
      await onSubmit?.({ payload, reservation })
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Unable to create reservation.')
      } else {
        setErrorMessage('Unable to create reservation.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="reservation-form" onSubmit={handleSubmit}>
      <div className="reservation-form-grid">
        <label>
          <span>Court Type</span>
          <select className="app-select" aria-label="Court Type" value={courtType} onChange={(event) => setCourtType(event.target.value)}>
            <option value="">Select sport</option>
            {courtTypeOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Venue Type</span>
          <select className="app-select" aria-label="Venue Type" value={venueType} onChange={(event) => setVenueType(event.target.value)}>
            <option value="">Select venue type</option>
            {venueTypeOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>

        <label className="reservation-form-grid__full">
          <span>Court</span>
          <select
            className="app-select"
            aria-label="Court"
            value={courtId}
            disabled={!canLoadCourts || isLoadingCourts}
            onChange={(event) => setCourtId(event.target.value)}
          >
            <option value="">
              {!canLoadCourts
                ? 'Select sport and venue type first'
                : isLoadingCourts
                  ? 'Loading available courts...'
                  : 'Select court'}
            </option>
            {courts.map((court) => (
              <option key={court.id} value={court.id}>
                {court.name} | {court.location} | {court.status}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Start Date</span>
          <input
            className="app-input"
            type="date"
            aria-label="Start Date"
            min={minDate}
            max={maxDate}
            value={startDate}
            onChange={(event) => {
              const nextDate = event.target.value
              setStartDate(nextDate)
              if (startClock) {
                const nextDateTime = combineDateAndTime(nextDate, startClock)
                const nextValue = nextDateTime ? new Date(`${nextDateTime}:00`) : null
                if (
                  !nextValue ||
                  nextValue < minimumReservationDate ||
                  nextValue > maximumReservationDate
                ) {
                  setStartClock('')
                }
              }
            }}
          />
        </label>

        <label>
          <span>Start Time</span>
          <select
            className="app-select"
            aria-label="Start Time"
            value={startClock}
            disabled={!startDate}
            onChange={(event) => setStartClock(event.target.value)}
          >
            <option value="">{startDate ? 'Select time' : 'Select date first'}</option>
            {availableTimeOptions.map((option) => (
              <option key={option} value={option}>
                {formatTimeLabel(option)}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Duration</span>
          <select className="app-select" aria-label="Duration" value={durationMinutes} onChange={(event) => setDurationMinutes(event.target.value)}>
            <option value="">Select duration</option>
            {allowedDurations.map((option) => (
              <option key={option} value={option}>
                {option} minutes
              </option>
            ))}
          </select>
        </label>
      </div>

      {selectedCourt ? (
        <section className="reservation-selected-court app-card">
          <div className="reservation-selected-court__row">
            <strong>{selectedCourt.name}</strong>
            <span className="app-badge app-badge-success">{selectedCourt.status}</span>
          </div>
          <div className="reservation-selected-court__meta">
            <span><MapPin size={14} /> {selectedCourt.location}</span>
            <span>Hourly Rate {formatCurrency(selectedCourt.hourlyRate)}</span>
            {computedAmountDue != null ? <span>Total {formatCurrency(computedAmountDue)}</span> : null}
            {computedEndTime ? <span><CalendarClock size={14} /> Ends {computedEndTime}</span> : null}
          </div>
        </section>
      ) : null}

      <section className="reservation-availability app-card">
        <div className="reservation-availability__header">
          <div>
            <h3>Availability Preview</h3>
            <p>Blocked slots are shown for the selected court and date.</p>
          </div>
        </div>

        {isLoadingAvailability ? <p className="reservation-feedback">Loading blocked slots...</p> : null}
        {!courtId || !startTime ? (
          <p className="reservation-feedback">Select a court and date/time to preview blocked slots.</p>
        ) : null}
        {!isLoadingAvailability && courtId && startTime && availability.length === 0 ? (
          <p className="reservation-feedback">No blocked slots for the selected date.</p>
        ) : null}
        {!isLoadingAvailability && availability.length > 0 ? (
          <div className="reservation-slot-list">
            {availability.map((slot) => (
              <article key={slot.reservationId} className="reservation-slot-item">
                <div className="reservation-slot-item__row">
                  <strong>{new Date(slot.startDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - {new Date(slot.endDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</strong>
                  <span className={slot.label === 'Paid' ? 'app-badge app-badge-success' : 'app-badge app-badge-warning'}>
                    {slot.label}
                  </span>
                </div>
                <span className="reservation-slot-item__meta">
                  {slot.reservationStatus} | {slot.paymentStatus}
                </span>
              </article>
            ))}
          </div>
        ) : null}
      </section>

      {errorMessage ? <p className="reservation-feedback reservation-feedback--error" role="alert">{errorMessage}</p> : null}
      {successMessage ? <p className="reservation-feedback reservation-feedback--success" role="status">{successMessage}</p> : null}
      {hasConflict ? <p className="reservation-feedback reservation-feedback--error">Selected time slot is no longer available.</p> : null}

      <button className="app-button reservation-submit" type="submit" disabled={isSubmitting || hasConflict}>
        {isSubmitting ? 'Creating reservation...' : 'Create reservation'}
      </button>
    </form>
  )
}

export default ReservationForm
