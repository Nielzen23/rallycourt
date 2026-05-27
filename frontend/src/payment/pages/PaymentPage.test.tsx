import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import PaymentPage from './PaymentPage'

const processPaymentMock = vi.fn()

vi.mock('../api/paymentApi', () => ({
  processPayment: (...args: unknown[]) => processPaymentMock(...args),
}))

describe('PaymentPage', () => {
  beforeEach(() => {
    processPaymentMock.mockReset()
  })

  it('renders payment methods', () => {
    render(
      <MemoryRouter>
        <PaymentPage />
      </MemoryRouter>,
    )

    expect(screen.getByLabelText(/card/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/gcash/i)).toBeInTheDocument()
  })

  it('submits payment request', async () => {
    processPaymentMock.mockResolvedValue({})

    render(
      <MemoryRouter>
        <PaymentPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: /submit payment/i }))

    await waitFor(() => {
      expect(processPaymentMock).toHaveBeenCalledWith({
        reservationId: 1,
        amount: 750,
      })
    })
  })

  it('handles success and failure states', async () => {
    processPaymentMock.mockRejectedValueOnce(new Error('failed'))

    render(
      <MemoryRouter>
        <PaymentPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: /submit payment/i }))

    expect(
      await screen.findByText('Payment failed. Try again.'),
    ).toBeInTheDocument()
  })
})
