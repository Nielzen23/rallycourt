import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import CourtManagementPage from './CourtManagementPage'

const fetchCourtManagementCourtsMock = vi.fn()
const fetchCourtManagementDetailsMock = vi.fn()

vi.mock('../api/courtManagementApi', () => ({
  fetchCourtManagementCourts: (...args: unknown[]) => fetchCourtManagementCourtsMock(...args),
  fetchCourtManagementDetails: (...args: unknown[]) => fetchCourtManagementDetailsMock(...args),
  createCourtManagementCourt: vi.fn(),
  updateCourtManagementCourt: vi.fn(),
  deleteCourtManagementCourt: vi.fn(),
}))

vi.mock('../../../court/components/CourtMap', () => ({
  default: () => <div>Map mock</div>,
}))

describe('CourtManagementPage', () => {
  beforeEach(() => {
    fetchCourtManagementCourtsMock.mockReset()
    fetchCourtManagementDetailsMock.mockReset()
  })

  it('renders court list and reservation payment statuses', async () => {
    fetchCourtManagementCourtsMock.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'Center Court',
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          location: 'Makati City',
          latitude: 14.55,
          longitude: 121.02,
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
        },
      ],
    })

    fetchCourtManagementDetailsMock.mockResolvedValue({
      id: 1,
      name: 'Center Court',
      courtType: 'BADMINTON',
      venueType: 'INDOOR',
      location: 'Makati City',
      latitude: 14.55,
      longitude: 121.02,
      status: 'AVAILABLE',
      openTime: '08:00:00',
      closeTime: '22:00:00',
      upcomingReservations: [
        {
          reservationId: 1,
          reservationDate: '2026-05-28',
          startTime: '18:00:00',
          durationMinutes: 90,
          paymentStatus: 'PAID',
          reservationStatus: 'CONFIRMED',
          playerName: 'playerone@rallycourt.local',
        },
      ],
    })

    render(
      <MemoryRouter>
        <CourtManagementPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('Center Court')).toBeInTheDocument()
    })

    expect(screen.getByText('PAID')).toBeInTheDocument()
    expect(screen.getByText('playerone@rallycourt.local')).toBeInTheDocument()
  })
})
