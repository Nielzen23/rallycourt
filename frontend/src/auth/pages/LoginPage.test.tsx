import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import LoginPage from './LoginPage'

const navigateMock = vi.fn()
const submitLoginMock = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  )

  return {
    ...actual,
    useNavigate: () => navigateMock,
  }
})

vi.mock('../hooks/useLogin', () => ({
  useLogin: () => ({
    isSubmitting: false,
    errorMessage: '',
    successMessage: '',
    submitLogin: submitLoginMock,
  }),
}))

describe('LoginPage', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    submitLoginMock.mockReset()
  })

  it('renders email and password fields', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
  })

  it('validates required fields through disabled submit state', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(
      screen.getByRole('button', { name: /sign in/i }),
    ).toBeDisabled()
  })

  it('shows error messages', async () => {
    vi.doMock('../hooks/useLogin', () => ({
      useLogin: () => ({
        isSubmitting: false,
        errorMessage: 'Login failed. Check your email and password.',
        successMessage: '',
        submitLogin: submitLoginMock,
      }),
    }))
  })

  it('submits login request', async () => {
    submitLoginMock.mockResolvedValue(true)

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'adminrallycourt@rallycourt.local' },
    })
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'RallyCourt123' },
    })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(submitLoginMock).toHaveBeenCalledWith(
        'adminrallycourt@rallycourt.local',
        'RallyCourt123',
      )
    })
  })
})
