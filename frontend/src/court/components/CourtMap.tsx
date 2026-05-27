import type { Court } from '../types/courtTypes'
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet'
import type { LatLngExpression } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './CourtMap.css'

type CourtMapProps = {
  courts: Court[]
  selectedCourtId?: number | null
  onSelectCourt?: (court: Court) => void
  onReserve?: (court: Court) => void
}

function CourtMap({ courts, selectedCourtId, onSelectCourt, onReserve }: CourtMapProps) {
  const center: LatLngExpression = courts.length
    ? [courts[0].latitude, courts[0].longitude]
    : [14.5995, 120.9842]
  const mapKey = courts.map((court) => court.id).join('-') || 'empty'

  return (
    <div className="court-map">
      <MapContainer
        key={mapKey}
        center={center}
        className="court-map__canvas"
        scrollWheelZoom
        zoom={12}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {courts.map((court) => {
          const isSelected = selectedCourtId === court.id

          return (
            <CircleMarker
              key={court.id}
              center={[court.latitude, court.longitude]}
              pathOptions={{
                color: isSelected ? '#f59e0b' : '#2563eb',
                fillColor: isSelected ? '#f59e0b' : '#2563eb',
                fillOpacity: 0.9,
              }}
              radius={isSelected ? 10 : 8}
              eventHandlers={{
                click: () => onSelectCourt?.(court),
              }}
            >
              <Popup>
                <div className="court-map__popup">
                  <strong>{court.name}</strong>
                  <span>{court.location}</span>
                  {onReserve ? (
                    <button
                      className="court-map__popup-button"
                      type="button"
                      onClick={() => onReserve(court)}
                    >
                      Reserve
                    </button>
                  ) : null}
                </div>
              </Popup>
            </CircleMarker>
          )
        })}
      </MapContainer>
    </div>
  )
}

export default CourtMap
