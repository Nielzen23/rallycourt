import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import ReservationForm from './ReservationForm'

const fetchCourts = vi.fn()
const fetchReservationConfig = vi.fn()
const fetchCourtAvailability = vi.fn()
const createReservation = vi.fn()

vi.mock('../../court/api/courtApi', () => ({
  fetchCourts: (...args: unknown[]) => fetchCourts(...args),
}))

vi.mock('../api/reservationApi', () => ({
  fetchReservationConfig: (...args: unknown[]) => fetchReservationConfig(...args),
  fetchCourtAvailability: (...args: unknown[]) => fetchCourtAvailability(...args),
  createReservation: (...args: unknown[]) => createReservation(...args),
}))

describe('ReservationForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchReservationConfig.mockResolvedValue({
      allowedDurationsMinutes: [60, 90, 120],
    })
    fetchCourts.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    fetchCourtAvailability.mockResolvedValue([])
    createReservation.mockResolvedValue({
      id: 1,
      courtId: 1,
      reservedBy: 'testGenerated1@generated.com',
      startTime: '2026-06-01T13:00:00',
      endTime: '2026-06-01T14:30:00',
      expiresAt: null,
      status: 'RESERVED_PENDING_PAYMENT',
      amountDue: 1125,
    })
  })

  it('disables court dropdown until filters are selected', () => {
    render(<ReservationForm />)

    expect(screen.getByLabelText(/court$/i)).toBeDisabled()
  })

  it('fetches courts after selecting court type and venue type', async () => {
    fetchCourts.mockResolvedValue({
      content: [
        { id: 1, name: 'Center Court', location: 'Makati', status: 'AVAILABLE', hourlyRate: 750 },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    render(<ReservationForm />)

    fireEvent.change(screen.getByLabelText(/court type/i), { target: { value: 'BADMINTON' } })
    fireEvent.change(screen.getByLabelText(/venue type/i), { target: { value: 'INDOOR' } })

    await waitFor(() => {
      expect(fetchCourts).toHaveBeenCalledWith({
        courtType: 'BADMINTON',
        venueType: 'INDOOR',
        status: 'AVAILABLE',
        page: 0,
        size: 20,
      })
    })
  })

  it('applies datetime min and max constraints', () => {
    render(<ReservationForm />)

    const input = screen.getByLabelText(/start date/i)
    expect(input).toHaveAttribute('min')
    expect(input).toHaveAttribute('max')
  })

  it('renders blocked slots and prevents conflicting submit', async () => {
    fetchCourts.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'Center Court',
          location: 'Makati',
          latitude: 0,
          longitude: 0,
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
          hourlyRate: 750,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    fetchCourtAvailability.mockResolvedValue([
      {
        reservationId: 10,
        startDateTime: '2026-06-01T10:00:00',
        endDateTime: '2026-06-01T11:30:00',
        reservationStatus: 'RESERVED_PENDING_PAYMENT',
        paymentStatus: 'PENDING',
        label: 'Booking Pending',
      },
    ])

    render(<ReservationForm />)

    fireEvent.change(screen.getByLabelText(/court type/i), { target: { value: 'BADMINTON' } })
    fireEvent.change(screen.getByLabelText(/venue type/i), { target: { value: 'INDOOR' } })

    await waitFor(() => expect(fetchCourts).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText(/court$/i), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: '2026-06-01' } })
    fireEvent.change(screen.getByLabelText(/^start time$/i), { target: { value: '10:30' } })
    fireEvent.change(screen.getByLabelText(/duration/i), { target: { value: '60' } })

    expect(await screen.findByText('Booking Pending')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /create reservation/i })).toBeDisabled()
  })

  it('submits a valid reservation payload', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    fetchCourts.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'Center Court',
          location: 'Makati',
          latitude: 0,
          longitude: 0,
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
          hourlyRate: 750,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    render(<ReservationForm onSubmit={onSubmit} />)

    fireEvent.change(screen.getByLabelText(/court type/i), { target: { value: 'BADMINTON' } })
    fireEvent.change(screen.getByLabelText(/venue type/i), { target: { value: 'INDOOR' } })
    await waitFor(() => expect(fetchCourts).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText(/court$/i), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: '2026-06-01' } })
    fireEvent.change(screen.getByLabelText(/^start time$/i), { target: { value: '13:00' } })
    fireEvent.change(screen.getByLabelText(/duration/i), { target: { value: '90' } })
    fireEvent.click(screen.getByRole('button', { name: /create reservation/i }))

    await waitFor(() => {
      expect(createReservation).toHaveBeenCalledWith({
        courtId: 1,
        startTime: '2026-06-01T13:00:00',
        durationMinutes: 90,
      })
      expect(onSubmit).toHaveBeenCalledWith({
        payload: {
          courtId: 1,
          startTime: '2026-06-01T13:00:00',
          durationMinutes: 90,
        },
        reservation: {
          id: 1,
          courtId: 1,
          reservedBy: 'testGenerated1@generated.com',
          startTime: '2026-06-01T13:00:00',
          endTime: '2026-06-01T14:30:00',
          expiresAt: null,
          status: 'RESERVED_PENDING_PAYMENT',
          amountDue: 1125,
        },
      })
    })
  })

  it('renders total booking amount based on selected duration', async () => {
    fetchCourts.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'Center Court',
          location: 'Makati',
          latitude: 0,
          longitude: 0,
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
          hourlyRate: 750,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    render(<ReservationForm />)

    fireEvent.change(screen.getByLabelText(/court type/i), { target: { value: 'BADMINTON' } })
    fireEvent.change(screen.getByLabelText(/venue type/i), { target: { value: 'INDOOR' } })
    await waitFor(() => expect(fetchCourts).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText(/court$/i), { target: { value: '1' } })

    expect(screen.getByText(/Hourly Rate/i)).toBeInTheDocument()
    expect(screen.queryByText(/Total/i)).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/duration/i), { target: { value: '90' } })

    expect(screen.getByText(/Total/i)).toBeInTheDocument()
    expect(screen.getByText(/1,125\.00/)).toBeInTheDocument()
  })
})
