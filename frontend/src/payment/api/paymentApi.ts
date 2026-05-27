import { apiClient } from '../../api/axios'

type ProcessPaymentRequest = {
  reservationId: number
  amount: number
}

export async function processPayment(request: ProcessPaymentRequest) {
  const response = await apiClient.post('/api/payments/process', request)
  return response.data
}
