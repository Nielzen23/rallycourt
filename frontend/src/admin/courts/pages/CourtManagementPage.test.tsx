import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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
          hourlyRate: 750,
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
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

    fireEvent.click(screen.getByRole('button', { name: 'Center Court' }))

    await waitFor(() => {
      expect(fetchCourtManagementDetailsMock).toHaveBeenCalledWith(1)
    })

    expect(screen.getByText('PAID')).toBeInTheDocument()
    expect(screen.getByText('playerone@rallycourt.local')).toBeInTheDocument()
  })

  it('supports pagination controls for court inventory', async () => {
    fetchCourtManagementCourtsMock
      .mockResolvedValueOnce({
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
            hourlyRate: 750,
          },
        ],
        page: 0,
        size: 10,
        totalElements: 12,
        totalPages: 2,
      })
      .mockResolvedValueOnce({
        content: [
          {
            id: 2,
            name: 'North Court',
            courtType: 'BASKETBALL',
            venueType: 'OUTDOOR',
            location: 'Quezon City',
            latitude: 14.67,
            longitude: 121.04,
            status: 'AVAILABLE',
            hourlyRate: 900,
          },
        ],
        page: 1,
        size: 10,
        totalElements: 12,
        totalPages: 2,
      })

    render(
      <MemoryRouter>
        <CourtManagementPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Center Court')).toBeInTheDocument()
    expect(screen.getByText(/12 courts \| Page 1 of 2/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Next court page' }))

    expect(await screen.findByText('North Court')).toBeInTheDocument()
    expect(screen.getByText(/12 courts \| Page 2 of 2/i)).toBeInTheDocument()
  })
})
