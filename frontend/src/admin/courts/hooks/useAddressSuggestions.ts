import { useEffect, useState } from 'react'
import axios from 'axios'
import { apiClient } from '../../../api/axios'
import type { ApiErrorResponse } from '../../../types/api'

export type AddressSuggestion = {
  address: string
  latitude: number
  longitude: number
}

export function useAddressSuggestions(query: string, enabled: boolean) {
  const [suggestions, setSuggestions] = useState<AddressSuggestion[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!enabled || query.trim().length < 3) {
      setSuggestions([])
      setIsLoading(false)
      setErrorMessage('')
      return
    }

    let active = true
    const timeoutId = window.setTimeout(async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const response = await apiClient.get<AddressSuggestion[]>(
          '/api/court-management/courts/address-suggestions',
          { params: { query: query.trim() } },
        )

        if (active) {
          setSuggestions(response.data)
        }
      } catch (error) {
        if (!active) {
          return
        }

        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setErrorMessage(
            error.response?.data?.message ?? 'Unable to load address suggestions.',
          )
        } else {
          setErrorMessage('Unable to load address suggestions.')
        }
        setSuggestions([])
      } finally {
        if (active) {
          setIsLoading(false)
        }
      }
    }, 250)

    return () => {
      active = false
      window.clearTimeout(timeoutId)
    }
  }, [enabled, query])

  return {
    suggestions,
    isLoading,
    errorMessage,
  }
}
