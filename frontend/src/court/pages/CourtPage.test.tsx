import { fireEvent, render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import CourtPage from './CourtPage'
import { MemoryRouter } from 'react-router-dom'

const useCourtsMock = vi.fn()
const navigateMock = vi.fn()

vi.mock('../hooks/useCourts', () => ({
  useCourts: () => useCourtsMock(),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  )

  return {
    ...actual,
    useNavigate: () => navigateMock,
  }
})

describe('CourtPage', () => {
  beforeEach(() => {
    useCourtsMock.mockReset()
    navigateMock.mockReset()
  })

  it('shows the sport prompt before any selection', () => {
    useCourtsMock.mockReturnValue({
      courts: [],
      isLoading: false,
      errorMessage: '',
      totalElements: 0,
    })

    render(
      <MemoryRouter>
        <CourtPage />
      </MemoryRouter>,
    )

    expect(
      screen.getByText('Select a sport to view available courts.'),
    ).toBeInTheDocument()
  })

  it('renders filtered court list after sport selection', () => {
    useCourtsMock.mockReturnValue({
      courts: [
        {
          id: 1,
          name: 'Center Court',
          location: 'Makati City',
          latitude: 14.55,
          longitude: 121.02,
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
        },
      ],
      isLoading: false,
      errorMessage: '',
      totalElements: 1,
    })

    render(
      <MemoryRouter>
        <CourtPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/sport/i), {
      target: { value: 'BADMINTON' },
    })

    expect(screen.getByText('Center Court')).toBeInTheDocument()
    expect(screen.getByText('Makati City')).toBeInTheDocument()
  })

  it('shows loading state', () => {
    useCourtsMock.mockReturnValue({
      courts: [],
      isLoading: true,
      errorMessage: '',
      totalElements: 0,
    })

    render(
      <MemoryRouter>
        <CourtPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/loading courts/i)).toBeInTheDocument()
  })

  it('handles API error', () => {
    useCourtsMock.mockReturnValue({
      courts: [],
      isLoading: false,
      errorMessage: 'Unable to load courts at the moment.',
      totalElements: 0,
    })

    render(
      <MemoryRouter>
        <CourtPage />
      </MemoryRouter>,
    )

    expect(
      screen.getByText('Unable to load courts at the moment.'),
    ).toBeInTheDocument()
  })

  it('navigates to reservation flow from reserve action', () => {
    useCourtsMock.mockReturnValue({
      courts: [
        {
          id: 7,
          name: 'Center Court',
          location: 'Makati City',
          latitude: 14.55,
          longitude: 121.02,
          courtType: 'BADMINTON',
          venueType: 'INDOOR',
          status: 'AVAILABLE',
          openTime: '08:00:00',
          closeTime: '22:00:00',
        },
      ],
      isLoading: false,
      errorMessage: '',
      totalElements: 1,
    })

    render(
      <MemoryRouter>
        <CourtPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/sport/i), {
      target: { value: 'BADMINTON' },
    })
    fireEvent.click(screen.getByRole('button', { name: /reserve/i }))

    expect(navigateMock).toHaveBeenCalledWith('/reservations/new?courtId=7')
  })
})
