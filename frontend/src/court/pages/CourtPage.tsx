import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Filter, MapPin, Trophy } from 'lucide-react'
import LoadingSpinner from '../../components/LoadingSpinner'
import PageLayout from '../../layouts/PageLayout'
import CourtMap from '../components/CourtMap'
import { useCourts } from '../hooks/useCourts'
import type { Court } from '../types/courtTypes'
import './CourtPage.css'

const COURT_TYPE_OPTIONS = [
  { value: 'BASKETBALL', label: 'Basketball' },
  { value: 'BADMINTON', label: 'Badminton' },
  { value: 'PICKLEBALL', label: 'Pickleball' },
  { value: 'FUTSAL', label: 'Futsal' },
]

const VENUE_TYPE_OPTIONS = [
  { value: '', label: 'All venues' },
  { value: 'INDOOR', label: 'Indoor' },
  { value: 'OUTDOOR', label: 'Outdoor' },
]

function CourtPage() {
  const navigate = useNavigate()
  const [courtType, setCourtType] = useState('')
  const [venueType, setVenueType] = useState('')
  const [selectedCourtId, setSelectedCourtId] = useState<number | null>(null)

  const filters = useMemo(
    () => ({
      courtType: courtType || undefined,
      venueType: venueType || undefined,
      status: 'AVAILABLE',
      page: 0,
      size: 10,
    }),
    [courtType, venueType],
  )

  const hasSelectedSport = courtType.length > 0
  const { courts, isLoading, errorMessage, totalElements } = useCourts(
    filters,
    hasSelectedSport,
  )

  const selectedCourt = courts.find((court) => court.id === selectedCourtId) ?? null

  const handleSelectCourt = (court: Court) => {
    setSelectedCourtId(court.id)
  }

  const handleReserveCourt = (court: Court) => {
    navigate(`/reservations/new?courtId=${court.id}`)
  }

  return (
    <PageLayout
      eyebrow="Court"
      title="Court Discovery"
      description="Select a sport first, then narrow the available courts by venue and location before moving to reservation."
    >
      <section className="court-page-grid">
        <div className="court-card">
          <div className="court-card__header">
            <h2>Find courts</h2>
            <p>Choose a sport to load matching courts on the list and map.</p>
          </div>

          <div className="court-filters">
            <label className="court-filter-field">
              <span>
                <Trophy size={16} /> Sport
              </span>
              <select
                aria-label="Sport"
                value={courtType}
                onChange={(event) => {
                  setCourtType(event.target.value)
                  setSelectedCourtId(null)
                }}
              >
                <option value="">Select a sport</option>
                {COURT_TYPE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="court-filter-field">
              <span>
                <Filter size={16} /> Venue
              </span>
              <select
                aria-label="Venue Type"
                value={venueType}
                onChange={(event) => {
                  setVenueType(event.target.value)
                  setSelectedCourtId(null)
                }}
                disabled={!hasSelectedSport}
              >
                {VENUE_TYPE_OPTIONS.map((option) => (
                  <option key={option.value || 'all'} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {!hasSelectedSport ? (
            <p className="court-empty-state">
              Select a sport to view available courts.
            </p>
          ) : null}

          {isLoading ? <LoadingSpinner label="Loading courts..." /> : null}
          {errorMessage ? <p className="court-card__error">{errorMessage}</p> : null}

          {!isLoading && !errorMessage && hasSelectedSport ? (
            <>
              <p className="court-results-meta">{totalElements} matching courts found.</p>
              {courts.length === 0 ? (
                <p className="court-empty-state">
                  No courts matched the selected filters.
                </p>
              ) : null}
            </>
          ) : null}

          {!isLoading && !errorMessage && courts.length > 0 ? (
            <ul className="court-list">
              {courts.map((court) => (
                <li
                  key={court.id}
                  className={
                    selectedCourtId === court.id
                      ? 'court-list__item court-list__item--selected'
                      : 'court-list__item'
                  }
                >
                  <button
                    className="court-list__button"
                    type="button"
                    onClick={() => handleSelectCourt(court)}
                  >
                    <strong>{court.name}</strong>
                    <span>{court.location}</span>
                    <small>{court.venueType} | {court.openTime} - {court.closeTime}</small>
                  </button>
                  <button
                    className="court-list__reserve"
                    type="button"
                    onClick={() => handleReserveCourt(court)}
                  >
                    Reserve
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </div>

        <div className="court-card">
          <div className="court-card__header">
            <h2>Map</h2>
            <p>
              <MapPin size={16} /> Showing only filtered court markers.
            </p>
          </div>
          {!hasSelectedSport ? (
            <p className="court-empty-state">
              Select a sport to load map markers.
            </p>
          ) : (
            <CourtMap
              courts={courts}
              selectedCourtId={selectedCourt?.id}
              onSelectCourt={handleSelectCourt}
              onReserve={handleReserveCourt}
            />
          )}
        </div>
      </section>
    </PageLayout>
  )
}

export default CourtPage
