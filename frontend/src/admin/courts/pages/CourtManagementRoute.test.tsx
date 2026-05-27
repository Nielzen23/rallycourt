import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import ProtectedRoute from '../../../components/ProtectedRoute'
import CourtManagementPage from './CourtManagementPage'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
} from '../../../utils/authStorage'

vi.mock('./CourtManagementPage', () => ({
  default: () => <div>Court management page</div>,
}))

describe('Court management route', () => {
  it('blocks non-manager users', () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'token')
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    render(
      <MemoryRouter initialEntries={['/court-management']}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route
            path="/court-management"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'COURT_OWNER']}>
                <CourtManagementPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })
})
