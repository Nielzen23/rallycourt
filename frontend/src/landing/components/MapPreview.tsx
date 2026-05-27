import { useState } from 'react'
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import type { LatLngExpression } from 'leaflet'
import type { LandingCourtMarker } from '../types/landingTypes'
import 'leaflet/dist/leaflet.css'

const fallbackMarkers: LandingCourtMarker[] = [
  {
    id: 1,
    name: 'Center Court',
    location: 'Makati City',
    latitude: 14.5547,
    longitude: 121.0244,
  },
  {
    id: 2,
    name: 'North Court',
    location: 'Quezon City',
    latitude: 14.676,
    longitude: 121.0437,
  },
]

function MapPreview() {
  const [markers] = useState<LandingCourtMarker[]>(fallbackMarkers)

  const center: LatLngExpression = markers.length
    ? [markers[0].latitude, markers[0].longitude]
    : [14.5995, 120.9842]

  return (
    <section className="grid gap-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <div className="grid gap-2">
        <h2 className="text-2xl font-semibold text-slate-900">Map Preview</h2>
        <p className="text-sm leading-7 text-slate-600">
          Guests can preview court locations before signing in.
        </p>
      </div>
      <div className="overflow-hidden rounded-lg border border-slate-200">
        <MapContainer
          center={center}
          className="h-[360px] w-full"
          scrollWheelZoom={false}
          zoom={11}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {markers.map((court) => (
            <Marker
              key={court.id}
              position={[court.latitude, court.longitude]}
            >
              <Popup>
                <strong>{court.name}</strong>
                <br />
                {court.location}
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
    </section>
  )
}

export default MapPreview
