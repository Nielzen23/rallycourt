declare module 'leaflet' {
  export type LatLngExpression = [number, number] | [number, number, number]
}

declare module 'react-leaflet' {
  import type { ComponentType, ReactNode } from 'react'

  export const MapContainer: ComponentType<{
    children?: ReactNode
    center?: unknown
    className?: string
    scrollWheelZoom?: boolean
    zoom?: number
  }>

  export const TileLayer: ComponentType<{
    attribution?: string
    url?: string
  }>

  export const Popup: ComponentType<{
    children?: ReactNode
  }>

  export const Marker: ComponentType<{
    children?: ReactNode
    position?: unknown
  }>

  export const CircleMarker: ComponentType<{
    children?: ReactNode
    center?: unknown
    eventHandlers?: Record<string, (...args: unknown[]) => void>
    key?: string | number
    pathOptions?: Record<string, unknown>
    radius?: number
  }>
}
