import { fireEvent, screen } from '@testing-library/react'
import DashboardPage from './DashboardPage'
import { renderWithRouter } from '../tests/testUtils'
import { AUTH_ROLE_STORAGE_KEY } from '../utils/authStorage'

describe('DashboardPage', () => {
  it('hides court management for players', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    renderWithRouter(<DashboardPage />)

    expect(screen.queryByText(/court management/i)).not.toBeInTheDocument()
    expect(screen.getByText(/reservations/i)).toBeInTheDocument()
  })

  it('shows court management for non-player roles', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'COURT_OWNER')

    renderWithRouter(<DashboardPage />)

    expect(screen.getByText(/court management/i)).toBeInTheDocument()
  })

  it('shows one court management entry for admins', () => {
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'ADMIN')

    renderWithRouter(<DashboardPage />)

    expect(screen.getAllByText(/court management/i)).toHaveLength(1)
  })

  it('renders reservation actions in the dashboard panel', () => {
    renderWithRouter(<DashboardPage />)

    expect(screen.getByRole('button', { name: 'Reservation' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Pending Payment' })).toBeDisabled()
    expect(screen.getByText('Center Court')).toBeInTheDocument()
  })

  it('opens and closes the reservation modal from the dashboard', () => {
    renderWithRouter(<DashboardPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Reservation' }))
    expect(screen.getByLabelText('Create reservation form')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Close reservation form' }))
    expect(screen.queryByLabelText('Create reservation form')).not.toBeInTheDocument()
  })
})
