import { useEffect, useState } from 'react'
import axios from 'axios'
import { fetchCourts } from '../api/courtApi'
import type { Court, CourtFilters } from '../types/courtTypes'
import type { ApiErrorResponse } from '../../types/api'

export function useCourts(filters: CourtFilters, enabled = true) {
  const [courts, setCourts] = useState<Court[]>([])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let isMounted = true

    if (!enabled) {
      setCourts([])
      setPage(0)
      setSize(filters.size ?? 10)
      setTotalElements(0)
      setTotalPages(0)
      setErrorMessage('')
      setIsLoading(false)
      return () => {
        isMounted = false
      }
    }

    const loadCourts = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const response = await fetchCourts(filters)

        if (isMounted) {
          setCourts(response.content)
          setPage(response.page)
          setSize(response.size)
          setTotalElements(response.totalElements)
          setTotalPages(response.totalPages)
        }
      } catch (error) {
        if (!isMounted) {
          return
        }

        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setErrorMessage(
            error.response?.data?.message ??
              'Unable to load courts at the moment.',
          )
        } else {
          setErrorMessage('Unexpected error while loading courts.')
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    void loadCourts()

    return () => {
      isMounted = false
    }
  }, [enabled, filters.courtType, filters.page, filters.size, filters.status, filters.venueType])

  return {
    courts,
    page,
    size,
    totalElements,
    totalPages,
    isLoading,
    errorMessage,
  }
}
