import { fireEvent, screen, within } from '@testing-library/react'
import { vi } from 'vitest'
import DashboardPage from './DashboardPage'
import { renderWithRouter } from '../tests/testUtils'
import { AUTH_ROLE_STORAGE_KEY } from '../utils/authStorage'

const fetchAllReservationsMock = vi.fn()
const fetchMyReservationsMock = vi.fn()
const processPaymentMock = vi.fn()
const fetchDashboardMock = vi.fn()

vi.mock('../reservation/api/reservationApi', () => ({
  fetchAllReservations: (...args: unknown[]) => fetchAllReservationsMock(...args),
  fetchMyReservations: (...args: unknown[]) => fetchMyReservationsMock(...args),
}))

vi.mock('../payment/api/paymentApi', () => ({
  processPayment: (...args: unknown[]) => processPaymentMock(...args),
}))

vi.mock('../dashboard/api/dashboardApi', () => ({
  fetchDashboard: (...args: unknown[]) => fetchDashboardMock(...args),
}))

const reservationPage = {
  content: [
    {
      id: 101,
      courtId: 8,
      courtName: 'Center Court',
      courtType: 'TENNIS',
      location: 'Makati, Metro Manila',
      latitude: 14.5547,
      longitude: 121.0244,
      reservedBy: 'player@rallycourt.test',
      contactName: 'Player, Rally',
      contactMobileNumber: '09171234567',
      startTime: '2099-04-02T09:00:00',
      endTime: '2099-04-02T10:00:00',
      expiresAt: '2099-04-02T09:30:00',
      status: 'RESERVED_PENDING_PAYMENT',
      paymentStatus: 'PENDING',
      amountDue: 750,
    },
    {
      id: 102,
      courtId: 9,
      courtName: 'South Court',
      courtType: 'BADMINTON',
      location: 'Pasig, Metro Manila',
      latitude: 14.5764,
      longitude: 121.0851,
      reservedBy: 'history@rallycourt.test',
      contactName: 'History, Booker',
      contactMobileNumber: '09981234567',
      startTime: '2024-04-01T08:00:00',
      endTime: '2024-04-01T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 850,
    },
    {
      id: 103,
      courtId: 10,
      courtName: 'Cancelled Court',
      courtType: 'PICKLEBALL',
      location: 'Taguig, Metro Manila',
      latitude: 14.5176,
      longitude: 121.0509,
      reservedBy: 'cancelled@rallycourt.test',
      contactName: 'Cancelled, Booker',
      contactMobileNumber: '09091234567',
      startTime: '2099-05-01T08:00:00',
      endTime: '2099-05-01T09:00:00',
      expiresAt: null,
      status: 'CANCELLED',
      paymentStatus: 'FAILED',
      amountDue: 900,
    },
    {
      id: 104,
      courtId: 11,
      courtName: 'East Court',
      courtType: 'TENNIS',
      location: 'Mandaluyong, Metro Manila',
      latitude: 14.5794,
      longitude: 121.0359,
      reservedBy: 'east@rallycourt.test',
      contactName: 'East, Booker',
      contactMobileNumber: '09180000001',
      startTime: '2099-04-03T08:00:00',
      endTime: '2099-04-03T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 750,
    },
    {
      id: 105,
      courtId: 12,
      courtName: 'West Court',
      courtType: 'BASKETBALL',
      location: 'Quezon City, Metro Manila',
      latitude: 14.676,
      longitude: 121.0437,
      reservedBy: 'west@rallycourt.test',
      contactName: 'West, Booker',
      contactMobileNumber: '09180000002',
      startTime: '2099-04-04T08:00:00',
      endTime: '2099-04-04T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 780,
    },
    {
      id: 106,
      courtId: 13,
      courtName: 'North Court',
      courtType: 'VOLLEYBALL',
      location: 'Marikina, Metro Manila',
      latitude: 14.6507,
      longitude: 121.1029,
      reservedBy: 'north@rallycourt.test',
      contactName: 'North, Booker',
      contactMobileNumber: '09180000003',
      startTime: '2099-04-05T08:00:00',
      endTime: '2099-04-05T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 790,
    },
    {
      id: 109,
      courtId: 16,
      courtName: 'Sky Court',
      courtType: 'TENNIS',
      location: 'San Juan, Metro Manila',
      latitude: 14.6019,
      longitude: 121.0355,
      reservedBy: 'sky@rallycourt.test',
      contactName: 'Sky, Booker',
      contactMobileNumber: '09180000006',
      startTime: '2099-04-06T08:00:00',
      endTime: '2099-04-06T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 795,
    },
    {
      id: 111,
      courtId: 18,
      courtName: 'Prime Court',
      courtType: 'BASKETBALL',
      location: 'Paranaque, Metro Manila',
      latitude: 14.4793,
      longitude: 121.0198,
      reservedBy: 'prime@rallycourt.test',
      contactName: 'Prime, Booker',
      contactMobileNumber: '09180000008',
      startTime: '2099-04-07T08:00:00',
      endTime: '2099-04-07T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 805,
    },
    {
      id: 107,
      courtId: 14,
      courtName: 'Legacy Court',
      courtType: 'BADMINTON',
      location: 'Pasay, Metro Manila',
      latitude: 14.5378,
      longitude: 120.9876,
      reservedBy: 'legacy@rallycourt.test',
      contactName: 'Legacy, Booker',
      contactMobileNumber: '09180000004',
      startTime: '2024-03-02T08:00:00',
      endTime: '2024-03-02T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 820,
    },
    {
      id: 108,
      courtId: 15,
      courtName: 'Archive Court',
      courtType: 'PICKLEBALL',
      location: 'Muntinlupa, Metro Manila',
      latitude: 14.4081,
      longitude: 121.0415,
      reservedBy: 'archive@rallycourt.test',
      contactName: 'Archive, Booker',
      contactMobileNumber: '09180000005',
      startTime: '2024-02-01T08:00:00',
      endTime: '2024-02-01T09:00:00',
      expiresAt: null,
      status: 'AUTO_CANCELLED',
      paymentStatus: 'FAILED',
      amountDue: 830,
    },
    {
      id: 110,
      courtId: 17,
      courtName: 'Retro Court',
      courtType: 'VOLLEYBALL',
      location: 'Las Pinas, Metro Manila',
      latitude: 14.4445,
      longitude: 120.9936,
      reservedBy: 'retro@rallycourt.test',
      contactName: 'Retro, Booker',
      contactMobileNumber: '09180000007',
      startTime: '2024-01-01T08:00:00',
      endTime: '2024-01-01T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 840,
    },
    {
      id: 112,
      courtId: 19,
      courtName: 'Classic Court',
      courtType: 'BADMINTON',
      location: 'Valenzuela, Metro Manila',
      latitude: 14.7006,
      longitude: 120.983,
      reservedBy: 'classic@rallycourt.test',
      contactName: 'Classic, Booker',
      contactMobileNumber: '09180000009',
      startTime: '2023-12-01T08:00:00',
      endTime: '2023-12-01T09:00:00',
      expiresAt: null,
      status: 'CONFIRMED',
      paymentStatus: 'SUCCESS',
      amountDue: 845,
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
}

const dashboardResponse = {
  totalBookings: 12,
  paidBookings: 7,
  unpaidBookings: 5,
  bookingPaymentBreakdown: [
    { label: 'Paid', count: 7 },
    { label: 'Unpaid', count: 5 },
  ],
  popularCourts: [
    { label: 'Center Court', count: 4 },
    { label: 'North Court', count: 3 },
  ],
  popularSports: [
    { label: 'TENNIS', count: 8 },
    { label: 'BADMINTON', count: 4 },
  ],
  userSummary: {
    usersWithBookings: 6,
    activeUsersWithoutBookings: 3,
    inactiveUsers: 2,
  },
}

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.clear()
    fetchAllReservationsMock.mockClear()
    fetchMyReservationsMock.mockClear()
    processPaymentMock.mockClear()
    fetchDashboardMock.mockClear()
    fetchAllReservationsMock.mockResolvedValue(reservationPage)
    fetchMyReservationsMock.mockResolvedValue(reservationPage)
    processPaymentMock.mockResolvedValue({})
    fetchDashboardMock.mockResolvedValue(dashboardResponse)
  })

  it('hides court management for players', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    renderWithRouter(<DashboardPage />)

    expect(screen.queryByText(/court management/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Book Court' })).toBeInTheDocument()
  })

  it('shows the ongoing reservations panel for non-player roles', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'COURT_OWNER')

    renderWithRouter(<DashboardPage />)

    expect(screen.getByRole('button', { name: 'Book Court' })).toBeInTheDocument()
  })

  it('shows the dashboard analytics sections for admins', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    expect(screen.getByText(/booking summary/i)).toBeInTheDocument()
  })

  it('renders reservation actions in the dashboard panel', async () => {
    renderWithRouter(<DashboardPage />)

    expect(screen.getByRole('button', { name: 'Book Court' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /pending payment/i })).toBeInTheDocument()
    expect(screen.getByText('Center Court')).toBeInTheDocument()
  })

  it('shows contact info for admin reservation items', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    expect(await screen.findByText('Player, Rally')).toBeInTheDocument()
    expect(screen.getByText('09171234567')).toBeInTheDocument()
  })

  it('allows admins to open pending payment details without proceeding to payment', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    fireEvent.click(await screen.findByRole('button', { name: /pending payment/i }))

    expect(screen.getByLabelText('Payment form')).toBeInTheDocument()
    expect(screen.getByText('Payments can only be completed by the player who made the reservation.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Payment unavailable' })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Payment unavailable' }))

    expect(processPaymentMock).not.toHaveBeenCalled()
  })

  it('shows reservation history for admins using past bookings', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    const historySection = await screen.findByLabelText('Reservation history list')

    expect(screen.getByText('Reservation History')).toBeInTheDocument()
    expect(within(historySection).getByRole('heading', { name: 'South Court' })).toBeInTheDocument()
    expect(within(historySection).getByText('History, Booker')).toBeInTheDocument()
    expect(within(historySection).getByText('09981234567')).toBeInTheDocument()
    expect(within(historySection).getByRole('heading', { name: 'Cancelled Court' })).toBeInTheDocument()
    expect(within(historySection).getByText('Cancelled, Booker')).toBeInTheDocument()
  })

  it('shows reservation history for players without admin contact details', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    renderWithRouter(<DashboardPage />)

    const historySection = await screen.findByLabelText('Reservation history list')

    expect(screen.getByText('Reservation History')).toBeInTheDocument()
    expect(within(historySection).getByRole('heading', { name: 'South Court' })).toBeInTheDocument()
    expect(within(historySection).queryByText('History, Booker')).not.toBeInTheDocument()
    expect(within(historySection).queryByText('09981234567')).not.toBeInTheDocument()
  })

  it('shows reservation history payment and cancellation status correctly', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    renderWithRouter(<DashboardPage />)

    const historySection = await screen.findByLabelText('Reservation history list')

    expect(within(historySection).getAllByText('Paid').length).toBeGreaterThan(0)
    expect(within(historySection).getByText('Cancelled')).toBeInTheDocument()
  })

  it('paginates ongoing reservations and reservation history with five cards per page', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    const ongoingSection = screen.getByLabelText('Reservations list')
    const historySection = screen.getByLabelText('Reservation history list')

    expect(await within(ongoingSection).findByRole('heading', { name: 'Center Court' })).toBeInTheDocument()
    expect(within(ongoingSection).getByRole('heading', { name: 'North Court' })).toBeInTheDocument()
    expect(within(ongoingSection).queryByText('Archive Court')).not.toBeInTheDocument()
    expect(screen.getAllByText(/page 1 of 2/i)).toHaveLength(2)
    expect(within(historySection).getByRole('heading', { name: 'Archive Court' })).toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: 'Next' })[1])

    expect(await within(historySection).findByRole('heading', { name: 'Classic Court' })).toBeInTheDocument()
    expect(within(historySection).queryByText('South Court')).not.toBeInTheDocument()
    expect(within(historySection).queryByText('Archive Court')).not.toBeInTheDocument()

    fireEvent.click(screen.getAllByRole('button', { name: 'Next' })[0])

    expect(await within(ongoingSection).findByRole('heading', { name: 'Prime Court' })).toBeInTheDocument()
    expect(within(ongoingSection).queryByText('Center Court')).not.toBeInTheDocument()
  })

  it('opens and closes the reservation modal from the dashboard', () => {
    renderWithRouter(<DashboardPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Book Court' }))
    expect(screen.getByLabelText('Create reservation form')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Close reservation form' }))
    expect(screen.queryByLabelText('Create reservation form')).not.toBeInTheDocument()
  })

  it('shows admin analytics cards and charts for admins', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    expect(await screen.findByText('Booking Summary')).toBeInTheDocument()
    expect(screen.getByText('Total bookings')).toBeInTheDocument()
    expect(screen.getByLabelText('Dashboard booking summary')).toHaveTextContent('12')
    expect(screen.getByText('Popular Courts')).toBeInTheDocument()
    expect(screen.getByText('Most Booked Sports')).toBeInTheDocument()
    expect(screen.getByText('User Activity')).toBeInTheDocument()
    expect(fetchDashboardMock).toHaveBeenCalledTimes(1)
  })

  it('does not load admin analytics for non-admin roles', async () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'COURT_OWNER')

    renderWithRouter(<DashboardPage />)

    await screen.findByText('Center Court')
    expect(fetchDashboardMock).not.toHaveBeenCalled()
    expect(screen.queryByText('Booking Summary')).not.toBeInTheDocument()
  })
})
