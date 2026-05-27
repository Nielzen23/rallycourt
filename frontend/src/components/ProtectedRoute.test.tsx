import { screen } from '@testing-library/react'
import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './ProtectedRoute'
import { renderWithRouter } from '../tests/testUtils'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
} from '../utils/authStorage'

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users', () => {
    renderWithRouter(
      <Routes>
        <Route path="/" element={<div>Login page</div>} />
        <Route
          path="/courts"
          element={
            <ProtectedRoute>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>,
      { route: '/courts' },
    )

    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('allows authenticated users', () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'token')

    renderWithRouter(
      <Routes>
        <Route
          path="/courts"
          element={
            <ProtectedRoute>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>,
      { route: '/courts' },
    )

    expect(screen.getByText('Protected content')).toBeInTheDocument()
  })

  it('redirects authenticated users without an allowed role', () => {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'token')
    localStorage.setItem(AUTH_ROLE_STORAGE_KEY, 'PLAYER')

    renderWithRouter(
      <Routes>
        <Route path="/dashboard" element={<div>Dashboard</div>} />
        <Route
          path="/courts"
          element={
            <ProtectedRoute allowedRoles={['ADMIN', 'COURT_OWNER', 'STAFF']}>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>,
      { route: '/courts' },
    )

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })
})
