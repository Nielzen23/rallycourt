import { useEffect, useState } from 'react'
import { CalendarDays, Pencil, Plus, Trash2 } from 'lucide-react'
import axios from 'axios'
import PageLayout from '../../../layouts/PageLayout'
import CourtMap from '../../../court/components/CourtMap'
import type { Court } from '../../../court/types/courtTypes'
import type { ApiErrorResponse } from '../../../types/api'
import { useAddressSuggestions } from '../hooks/useAddressSuggestions'
import {
  createCourtManagementCourt,
  deleteCourtManagementCourt,
  fetchCourtManagementDetails,
  fetchCourtManagementCourts,
  updateCourtManagementCourt,
} from '../api/courtManagementApi'
import type {
  CourtManagementCourt,
  CourtManagementDetails,
  CourtManagementRequest,
} from '../types/courtManagementTypes'
import './CourtManagementPage.css'

const courtTypeOptions = ['BASKETBALL', 'BADMINTON', 'PICKLEBALL', 'FUTSAL']
const venueTypeOptions = ['INDOOR', 'OUTDOOR']
const statusOptions = ['AVAILABLE', 'UNAVAILABLE']

const emptyForm: CourtManagementRequest = {
  name: '',
  courtType: 'BADMINTON',
  venueType: 'INDOOR',
  location: '',
  status: 'AVAILABLE',
  hourlyRate: 750,
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'PHP' }).format(value)
}

function CourtManagementPage() {
  const pageSize = 10
  const [courts, setCourts] = useState<CourtManagementCourt[]>([])
  const [mapCourts, setMapCourts] = useState<Court[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [selectedCourtId, setSelectedCourtId] = useState<number | null>(null)
  const [selectedCourtDetails, setSelectedCourtDetails] = useState<CourtManagementDetails | null>(null)
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [mapErrorMessage, setMapErrorMessage] = useState('')
  const [isMapLoading, setIsMapLoading] = useState(true)
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [editingCourt, setEditingCourt] = useState<CourtManagementCourt | null>(null)
  const [formState, setFormState] = useState<CourtManagementRequest>(emptyForm)
  const [isLocationFocused, setIsLocationFocused] = useState(false)
  const [filters, setFilters] = useState({
    courtType: '',
    venueType: '',
    status: '',
  })
  const {
    suggestions: addressSuggestions,
    isLoading: isAddressSuggestionsLoading,
  } = useAddressSuggestions(formState.location, isFormModalOpen && isLocationFocused)

  const selectedCourt =
    selectedCourtDetails ??
    mapCourts.find((court) => court.id === selectedCourtId) ??
    courts.find((court) => court.id === selectedCourtId) ??
    null

  const loadCourts = async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const response = await fetchCourtManagementCourts({
        courtType: filters.courtType || undefined,
        venueType: filters.venueType || undefined,
        status: filters.status || undefined,
        page,
        size: pageSize,
      })
      setCourts(response.content)
      setPage(response.page)
      setTotalPages(response.totalPages)
      setTotalElements(response.totalElements)
      if (response.content.length === 0) {
        setSelectedCourtId(null)
        setSelectedCourtDetails(null)
        setIsDetailsModalOpen(false)
      } else if (selectedCourtId && !response.content.some((court) => court.id === selectedCourtId)) {
        setSelectedCourtId(null)
        setSelectedCourtDetails(null)
        setIsDetailsModalOpen(false)
      }
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Unable to load court management data.')
      } else {
        setErrorMessage('Unable to load court management data.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const loadMapCourts = async () => {
    setIsMapLoading(true)
    setMapErrorMessage('')

    try {
      const mapPageSize = 50
      const firstPage = await fetchCourtManagementCourts({
        courtType: filters.courtType || undefined,
        venueType: filters.venueType || undefined,
        status: filters.status || undefined,
        page: 0,
        size: mapPageSize,
      })
      const remainingPageIndexes = Array.from(
        { length: Math.max(firstPage.totalPages - 1, 0) },
        (_, index) => index + 1,
      )
      const remainingPages = await Promise.all(
        remainingPageIndexes.map((currentPage) =>
          fetchCourtManagementCourts({
            courtType: filters.courtType || undefined,
            venueType: filters.venueType || undefined,
            status: filters.status || undefined,
            page: currentPage,
            size: mapPageSize,
          }),
        ),
      )
      const allCourts = [firstPage, ...remainingPages].flatMap((response) => response.content)
      const nextMapCourts = allCourts.map<Court>((court) => ({
        id: court.id,
        name: court.name,
        location: court.location,
        latitude: court.latitude,
        longitude: court.longitude,
        courtType: court.courtType,
        venueType: court.venueType,
        status: court.status,
        openTime: court.openTime ?? '08:00:00',
        closeTime: court.closeTime ?? '22:00:00',
        hourlyRate: court.hourlyRate,
      }))

      setMapCourts(nextMapCourts)
      setSelectedCourtId((currentSelectedCourtId) => {
        if (currentSelectedCourtId && nextMapCourts.some((court) => court.id === currentSelectedCourtId)) {
          return currentSelectedCourtId
        }
        return nextMapCourts[0]?.id ?? null
      })
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setMapErrorMessage(error.response?.data?.message ?? 'Unable to load court map preview.')
      } else {
        setMapErrorMessage('Unable to load court map preview.')
      }
      setMapCourts([])
      setSelectedCourtId(null)
    } finally {
      setIsMapLoading(false)
    }
  }

  useEffect(() => {
    setPage(0)
  }, [filters.courtType, filters.status, filters.venueType])

  useEffect(() => {
    void loadCourts()
  }, [filters.courtType, filters.status, filters.venueType, page])

  useEffect(() => {
    void loadMapCourts()
  }, [filters.courtType, filters.status, filters.venueType])

  useEffect(() => {
    if (!selectedCourtId || !isDetailsModalOpen) {
      return
    }

    let active = true
    const loadCourtDetails = async () => {
      try {
        const response = await fetchCourtManagementDetails(selectedCourtId)
        if (active) {
          setSelectedCourtDetails(response)
        }
      } catch (error) {
        if (!active) {
          return
        }

        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setErrorMessage(error.response?.data?.message ?? 'Unable to load court details.')
        } else {
          setErrorMessage('Unable to load court details.')
        }
      }
    }

    void loadCourtDetails()

    return () => {
      active = false
    }
  }, [isDetailsModalOpen, selectedCourtId])

  const openCreateModal = () => {
    setEditingCourt(null)
    setFormState(emptyForm)
    setIsFormModalOpen(true)
    setSuccessMessage('')
    setErrorMessage('')
    setIsLocationFocused(false)
  }

  const openEditModal = (court: CourtManagementCourt) => {
    setEditingCourt(court)
    setFormState({
      name: court.name,
      courtType: court.courtType,
      venueType: court.venueType,
      location: court.location,
      status: court.status,
      hourlyRate: court.hourlyRate,
    })
    setIsFormModalOpen(true)
    setSuccessMessage('')
    setErrorMessage('')
    setIsLocationFocused(false)
  }

  const openDetailsModal = (courtId: number) => {
    setSelectedCourtId(courtId)
    setSelectedCourtDetails(null)
    setIsDetailsModalOpen(true)
    setErrorMessage('')
    setSuccessMessage('')
  }

  const handleSaveCourt = async () => {
    setIsSaving(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      if (editingCourt) {
        await updateCourtManagementCourt(editingCourt.id, formState)
        setSuccessMessage('Court updated.')
      } else {
        await createCourtManagementCourt(formState)
        setSuccessMessage('Court created.')
      }

      setIsFormModalOpen(false)
      await loadCourts()
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Unable to save court.')
      } else {
        setErrorMessage('Unable to save court.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  const handleDeleteCourt = async () => {
    if (!selectedCourtId) {
      return
    }

    setIsSaving(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await deleteCourtManagementCourt(selectedCourtId)
      setIsDeleteModalOpen(false)
      setSelectedCourtDetails(null)
      setSuccessMessage('Court deleted.')
      await loadCourts()
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Unable to delete court.')
      } else {
        setErrorMessage('Unable to delete court.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <PageLayout
      eyebrow="Admin"
      title="Court Management"
      description="Manage courts, review upcoming reservations, and inspect payment visibility for each managed venue."
    >
      <section className="admin-courts-page">
        <div className="admin-courts-panel app-card">
          <div className="admin-courts-panel__header">
            <div>
              <h2>Court Inventory</h2>
              <p>Admin-managed list of courts and operational status.</p>
            </div>
            <button className="admin-primary-button app-button" type="button" onClick={openCreateModal}>
              <Plus size={16} /> Add Court
            </button>
          </div>

          <div className="admin-courts-filters">
            <select
              className="app-select"
              aria-label="Admin Court Type Filter"
              value={filters.courtType}
              onChange={(event) => setFilters((current) => ({ ...current, courtType: event.target.value }))}
            >
              <option value="">All sports</option>
              {courtTypeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <select
              className="app-select"
              aria-label="Admin Venue Type Filter"
              value={filters.venueType}
              onChange={(event) => setFilters((current) => ({ ...current, venueType: event.target.value }))}
            >
              <option value="">All venues</option>
              {venueTypeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <select
              className="app-select"
              aria-label="Admin Status Filter"
              value={filters.status}
              onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}
            >
              <option value="">All statuses</option>
              {statusOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>

          {successMessage ? <p className="admin-feedback admin-feedback--success">{successMessage}</p> : null}
          {errorMessage ? <p className="admin-feedback admin-feedback--error">{errorMessage}</p> : null}
          {isLoading ? <p className="admin-placeholder">Loading courts...</p> : null}

          {!isLoading ? (
            <>
              <div className="admin-courts-table-wrap app-table-wrap">
                <table className="admin-courts-table app-table">
                  <thead>
                    <tr>
                      <th>Court Name</th>
                      <th>Court Type</th>
                      <th>Venue Type</th>
                      <th>Status</th>
                      <th>Location</th>
                    </tr>
                  </thead>
                  <tbody>
                    {courts.map((court) => (
                      <tr
                        key={court.id}
                        className={selectedCourtId === court.id && isDetailsModalOpen ? 'is-selected' : undefined}
                      >
                        <td>
                          <button
                            className="admin-courts-name-button"
                            type="button"
                            onClick={() => openDetailsModal(court.id)}
                          >
                            {court.name}
                          </button>
                        </td>
                        <td>{court.courtType}</td>
                        <td>{court.venueType}</td>
                        <td>
                          <span
                            className={
                              court.status === 'AVAILABLE'
                                ? 'admin-status-badge admin-status-badge--available app-badge app-badge-success'
                                : 'admin-status-badge admin-status-badge--unavailable app-badge app-badge-warning'
                            }
                          >
                            {court.status}
                          </span>
                        </td>
                        <td>{court.location}</td>
                      </tr>
                    ))}
                    {courts.length === 0 ? (
                      <tr>
                        <td colSpan={6}>
                          <p className="admin-placeholder">No courts matched the current filters.</p>
                        </td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
              <div className="admin-courts-pagination" aria-label="Court management pagination">
                <span className="admin-placeholder">
                  {totalElements} courts | Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
                </span>
                <div className="admin-courts-pagination__actions">
                <button
                  className="app-button-secondary"
                  type="button"
                  aria-label="Previous court page"
                  disabled={page === 0}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                >
                    Previous
                  </button>
                <button
                  className="app-button-secondary"
                  type="button"
                  aria-label="Next court page"
                  disabled={totalPages === 0 || page >= totalPages - 1}
                  onClick={() => setPage((current) => current + 1)}
                >
                    Next
                  </button>
                </div>
              </div>
            </>
          ) : null}
        </div>
        <div className="admin-courts-panel app-card">
          <div className="admin-courts-panel__header">
            <div>
              <h2>Map Preview</h2>
              <p>Registered court locations for the current filters. Select a marker to inspect the court details.</p>
            </div>
          </div>
          {mapErrorMessage ? <p className="admin-feedback admin-feedback--error">{mapErrorMessage}</p> : null}
          {isMapLoading ? <p className="admin-placeholder">Loading court map preview...</p> : null}
          {!isMapLoading && !mapErrorMessage && mapCourts.length === 0 ? (
            <p className="admin-placeholder">No courts matched the current filters.</p>
          ) : null}
          {!isMapLoading && !mapErrorMessage && mapCourts.length > 0 ? (
            <div className="admin-map-preview">
              <div className="admin-map-wrap">
                <CourtMap
                  courts={mapCourts}
                  selectedCourtId={selectedCourtId}
                  onSelectCourt={(court) => {
                    setSelectedCourtId(court.id)
                    setSelectedCourtDetails(null)
                    setIsDetailsModalOpen(true)
                  }}
                />
              </div>
              {selectedCourt ? (
                <article className="admin-map-preview__details" aria-label="Selected map court details">
                  <div className="admin-map-preview__header">
                    <h3>{selectedCourt.name}</h3>
                    <span>{selectedCourt.status}</span>
                  </div>
                  <dl>
                    <div>
                      <dt>Sport</dt>
                      <dd>{selectedCourt.courtType}</dd>
                    </div>
                    <div>
                      <dt>Venue</dt>
                      <dd>{selectedCourt.venueType}</dd>
                    </div>
                    <div>
                      <dt>Location</dt>
                      <dd>{selectedCourt.location}</dd>
                    </div>
                    <div>
                      <dt>Hours</dt>
                      <dd>{selectedCourt.openTime} - {selectedCourt.closeTime}</dd>
                    </div>
                    <div>
                      <dt>Rate</dt>
                      <dd>{formatCurrency(selectedCourt.hourlyRate)}/hour</dd>
                    </div>
                  </dl>
                </article>
              ) : null}
            </div>
          ) : null}
        </div>
      </section>

      {isDetailsModalOpen ? (
        <div className="admin-modal-backdrop app-modal-backdrop" role="presentation" onClick={() => setIsDetailsModalOpen(false)}>
          <section className="admin-modal app-modal admin-details-modal" aria-label="Court details" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal__header app-modal-header">
              <div>
                <span className="app-eyebrow">Court Details</span>
                <h2>{selectedCourt?.name ?? 'Loading court details'}</h2>
                <p className="admin-placeholder">Selected court information, map position, and upcoming reservations.</p>
              </div>
                <div className="admin-modal__header-actions">
                  {selectedCourt ? (
                    <>
                      <button
                        className="app-button-secondary"
                        type="button"
                        onClick={() => {
                          setIsDetailsModalOpen(false)
                          openEditModal(selectedCourt)
                        }}
                      >
                        <Pencil size={16} /> Edit
                      </button>
                      <button
                        type="button"
                        className="app-button-danger"
                        onClick={() => {
                          setSelectedCourtId(selectedCourt.id)
                          setIsDetailsModalOpen(false)
                          setIsDeleteModalOpen(true)
                        }}
                      >
                        <Trash2 size={16} /> Remove Court
                      </button>
                    </>
                  ) : null}
                  <button type="button" className="admin-close-button app-close-button" onClick={() => setIsDetailsModalOpen(false)}>
                    Close
                  </button>
                </div>
              </div>

            {selectedCourt ? (
              <div className="admin-details-stack">
                <div className="admin-details-grid">
                  <div>
                    <span>Name</span>
                    <strong>{selectedCourt.name}</strong>
                  </div>
                  <div>
                    <span>Sport</span>
                    <strong>{selectedCourt.courtType}</strong>
                  </div>
                  <div>
                    <span>Venue</span>
                    <strong>{selectedCourt.venueType}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>{selectedCourt.status}</strong>
                  </div>
                  <div>
                    <span>Location</span>
                    <strong>{selectedCourt.location}</strong>
                  </div>
                  <div>
                    <span>Coordinates</span>
                    <strong>{selectedCourt.latitude}, {selectedCourt.longitude}</strong>
                  </div>
                </div>

                <div className="admin-map-wrap">
                  <CourtMap
                    courts={mapCourts}
                    selectedCourtId={selectedCourt.id}
                    onSelectCourt={(court) => setSelectedCourtId(court.id)}
                  />
                </div>

                <div className="admin-reservations-section">
                  <div className="admin-section-title">
                    <h3>
                      <CalendarDays size={16} /> Upcoming Reservations
                    </h3>
                  </div>
                  <div className="admin-reservations-table-wrap">
                    <table className="admin-reservations-table app-table">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Start Time</th>
                          <th>Duration</th>
                          <th>Reservation Status</th>
                          <th>Payment Status</th>
                          <th>Player</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedCourtDetails?.upcomingReservations.map((reservation) => (
                          <tr key={reservation.reservationId}>
                            <td>{reservation.reservationDate}</td>
                            <td>{reservation.startTime}</td>
                            <td>{reservation.durationMinutes} min</td>
                            <td>{reservation.reservationStatus}</td>
                            <td>
                              <span
                                className={
                                  reservation.paymentStatus === 'PAID'
                                    ? 'admin-payment-badge admin-payment-badge--paid app-badge app-badge-success'
                                    : 'admin-payment-badge admin-payment-badge--pending app-badge app-badge-warning'
                                }
                              >
                                {reservation.paymentStatus}
                              </span>
                            </td>
                            <td>{reservation.playerName}</td>
                          </tr>
                        ))}
                        {selectedCourtDetails?.upcomingReservations.length === 0 ? (
                          <tr>
                            <td colSpan={6}>
                              <p className="admin-placeholder">No upcoming reservations for this court.</p>
                            </td>
                          </tr>
                        ) : null}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            ) : (
              <p className="admin-placeholder">Loading court details...</p>
            )}
          </section>
        </div>
      ) : null}

      {isFormModalOpen ? (
        <div className="admin-modal-backdrop app-modal-backdrop" role="presentation" onClick={() => setIsFormModalOpen(false)}>
          <section className="admin-modal app-modal" aria-label="Court form" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal__header app-modal-header">
              <div>
                <span className="app-eyebrow">{editingCourt ? 'Edit Court' : 'Add Court'}</span>
                <h2>{editingCourt ? 'Update court' : 'Create court'}</h2>
              </div>
              <button type="button" className="admin-close-button app-close-button" onClick={() => setIsFormModalOpen(false)}>
                Close
              </button>
            </div>
            <div className="admin-form-grid">
              <label>
                <span>Name</span>
                <input
                  className="app-input"
                  value={formState.name}
                  onChange={(event) => setFormState((current) => ({ ...current, name: event.target.value }))}
                />
              </label>
              <label>
                <span>Location</span>
                <div className="admin-location-field">
                  <input
                    className="app-input"
                    value={formState.location}
                    onFocus={() => setIsLocationFocused(true)}
                    onBlur={() => {
                      window.setTimeout(() => setIsLocationFocused(false), 120)
                    }}
                    onChange={(event) =>
                      setFormState((current) => ({ ...current, location: event.target.value }))
                    }
                    placeholder="Search address"
                  />
                  {isLocationFocused && (formState.location.trim().length >= 3 || isAddressSuggestionsLoading) ? (
                    <div className="admin-location-suggestions">
                      {isAddressSuggestionsLoading ? (
                        <div className="admin-location-suggestion admin-location-suggestion--muted">
                          Loading suggestions...
                        </div>
                      ) : null}
                      {!isAddressSuggestionsLoading && addressSuggestions.length === 0 ? (
                        <div className="admin-location-suggestion admin-location-suggestion--muted">
                          No address suggestions found.
                        </div>
                      ) : null}
                      {addressSuggestions.map((suggestion) => (
                        <button
                          key={`${suggestion.address}-${suggestion.latitude}-${suggestion.longitude}`}
                          type="button"
                          className="admin-location-suggestion"
                          onMouseDown={(event) => event.preventDefault()}
                          onClick={() => {
                            setFormState((current) => ({
                              ...current,
                              location: suggestion.address,
                            }))
                            setIsLocationFocused(false)
                          }}
                        >
                          {suggestion.address}
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              </label>
              <label>
                <span>Court Type</span>
                <select
                  className="app-select"
                  value={formState.courtType}
                  onChange={(event) => setFormState((current) => ({ ...current, courtType: event.target.value }))}
                >
                  {courtTypeOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Venue Type</span>
                <select
                  className="app-select"
                  value={formState.venueType}
                  onChange={(event) => setFormState((current) => ({ ...current, venueType: event.target.value }))}
                >
                  {venueTypeOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Status</span>
                <select
                  className="app-select"
                  value={formState.status}
                  onChange={(event) => setFormState((current) => ({ ...current, status: event.target.value }))}
                >
                  {statusOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>Hourly Rate</span>
                <input
                  className="app-input"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={formState.hourlyRate}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      hourlyRate: Number(event.target.value),
                    }))
                  }
                />
              </label>
            </div>
            <div className="admin-modal__actions">
              <button type="button" className="admin-secondary-button app-button-secondary" onClick={() => setIsFormModalOpen(false)}>
                Cancel
              </button>
              <button type="button" className="admin-primary-button app-button" onClick={handleSaveCourt} disabled={isSaving}>
                {isSaving ? 'Saving...' : editingCourt ? 'Save Changes' : 'Create Court'}
              </button>
            </div>
          </section>
        </div>
      ) : null}

      {isDeleteModalOpen ? (
        <div className="admin-modal-backdrop app-modal-backdrop" role="presentation" onClick={() => setIsDeleteModalOpen(false)}>
          <section className="admin-modal admin-modal--compact app-modal app-modal-compact" aria-label="Delete confirmation" onClick={(event) => event.stopPropagation()}>
            <div className="admin-modal__header app-modal-header">
              <div>
                <span className="app-eyebrow">Remove Court</span>
                <h2>Confirm removal</h2>
              </div>
            </div>
            <p className="admin-delete-copy">
              Remove <strong>{selectedCourt?.name ?? 'this court'}</strong>? This action is blocked when active future reservations still exist.
            </p>
            <div className="admin-modal__actions">
              <button type="button" className="admin-secondary-button app-button-secondary" onClick={() => setIsDeleteModalOpen(false)}>
                Cancel
              </button>
              <button type="button" className="admin-danger-button app-button-danger" onClick={handleDeleteCourt} disabled={isSaving}>
                {isSaving ? 'Removing...' : 'Remove Court'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </PageLayout>
  )
}

export default CourtManagementPage
