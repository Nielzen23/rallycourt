import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { vi } from 'vitest'
import ProtectedRoute from '../../../components/ProtectedRoute'
import CourtManagementPage from './CourtManagementPage'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
} from '../../../utils/authStorage'

function createToken(expSecondsFromNow = 3600) {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + expSecondsFromNow }))
  return `${header}.${payload}.signature`
}

vi.mock('./CourtManagementPage', () => ({
  default: () => <div>Court management page</div>,
}))

describe('Court management route', () => {
  it('blocks non-manager users', () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, createToken())
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    render(
      <MemoryRouter initialEntries={['/court-management']}>
        <Routes>
          <Route path="/session-expired" element={<div>Session expired</div>} />
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
