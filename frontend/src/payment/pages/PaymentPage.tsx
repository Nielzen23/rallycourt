import { useState } from 'react'
import type { FormEvent } from 'react'
import PageLayout from '../../layouts/PageLayout'
import { processPayment } from '../api/paymentApi'
import './PaymentPage.css'

function PaymentPage() {
  const [paymentMethod, setPaymentMethod] = useState('CARD')
  const [statusMessage, setStatusMessage] = useState('')
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    try {
      await processPayment({
        reservationId: 1,
        amount: 750,
      })
      setStatusMessage('Payment processed successfully.')
    } catch {
      setStatusMessage('Payment failed. Try again.')
    }
  }

  return (
    <PageLayout
      eyebrow="Payment"
      title="Payment"
      description="MVP payment submission flow for reservation confirmation."
    >
      <section className="payment-toolbar">
        <button
          className="payment-action"
          type="button"
          onClick={() => setIsPaymentModalOpen(true)}
        >
          Open payment form
        </button>
      </section>
      {statusMessage ? <p className="payment-status" role="status">{statusMessage}</p> : null}
      {isPaymentModalOpen ? (
        <div
          className="payment-modal-backdrop"
          role="presentation"
          onClick={() => setIsPaymentModalOpen(false)}
        >
          <section
            className="payment-modal"
            aria-label="Payment form"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              className="payment-modal-close"
              type="button"
              aria-label="Close payment form"
              onClick={() => setIsPaymentModalOpen(false)}
            >
              ×
            </button>
            <div className="payment-modal-header">
              <span className="payment-modal-eyebrow">Payment</span>
              <h2>Submit payment</h2>
              <p>Choose a payment method to complete the reservation flow.</p>
            </div>
            <form className="payment-form" onSubmit={handleSubmit}>
              <fieldset>
                <legend>Payment method</legend>
                <label>
                  <input
                    checked={paymentMethod === 'CARD'}
                    name="payment-method"
                    type="radio"
                    onChange={() => setPaymentMethod('CARD')}
                  />
                  Card
                </label>
                <label>
                  <input
                    checked={paymentMethod === 'GCASH'}
                    name="payment-method"
                    type="radio"
                    onChange={() => setPaymentMethod('GCASH')}
                  />
                  GCash
                </label>
              </fieldset>

              <button type="submit">Submit payment</button>
            </form>
          </section>
        </div>
      ) : null}
    </PageLayout>
  )
}

export default PaymentPage
