import { createBrowserRouter } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import LoginPage from './auth/pages/LoginPage'
import RegisterPage from './auth/pages/RegisterPage'
import CourtPage from './court/pages/CourtPage'
import ReservationPage from './reservation/pages/ReservationPage'
import ProtectedRoute from './components/ProtectedRoute'
import DashboardPage from './routes/DashboardPage'
import LandingPage from './landing/pages/LandingPage'
import CourtManagementPage from './admin/courts/pages/CourtManagementPage'
import SessionExpiredPage from './routes/SessionExpiredPage'
import UserActivityPage from './admin/activity/pages/UserActivityPage'

export const router = createBrowserRouter([
  {
    path: '/',
    children: [
      {
        index: true,
        element: <LandingPage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        path: 'register',
        element: <RegisterPage />,
      },
      {
        path: 'session-expired',
        element: <SessionExpiredPage />,
      },
      {
        path: 'courts',
        element: (
          <ProtectedRoute allowedRoles={['ADMIN', 'COURT_OWNER', 'STAFF']}>
            <CourtPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    element: <AppLayout />,
    children: [
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'reservations',
        element: (
          <ProtectedRoute>
            <ReservationPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'reservations/new',
        element: (
          <ProtectedRoute>
            <ReservationPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'court-management',
        element: (
          <ProtectedRoute allowedRoles={['ADMIN', 'COURT_OWNER']}>
            <CourtManagementPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'user-activity',
        element: (
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <UserActivityPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
])
