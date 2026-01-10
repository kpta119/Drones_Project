"use client";

import { useMemo, useRef } from "react";
import { MapContainer, TileLayer, Marker, Circle } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { RegistrationData } from "./operator_register_module";

import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

let DefaultIcon = L.icon({
  iconUrl: (markerIcon as any).src || markerIcon,
  shadowUrl: (markerShadow as any).src || markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});
L.Marker.prototype.options.icon = DefaultIcon;

interface LocationInputModuleProps {
  onNext: () => void;
  onPrev: () => void;
  data: RegistrationData;
  setData: (data: RegistrationData) => void;
}

export function LocationInputModule({
  onNext,
  onPrev,
  data,
  setData,
}: LocationInputModuleProps) {
  const markerRef = useRef<L.Marker>(null);

  const getEdgePoint = (
    center: { lat: number; lng: number },
    radiusMeters: number
  ) => {
    const latChange = radiusMeters / 111320;
    return {
      lat: center.lat,
      lng: center.lng + latChange / Math.cos(center.lat * (Math.PI / 180)),
    };
  };

  const edgePoint = useMemo(
    () => getEdgePoint(data.coordinates, data.radius),
    [data.coordinates, data.radius]
  );

  const updateCoords = (lat: number, lng: number) => {
    setData({ ...data, coordinates: { lat, lng } });
  };

  const updateRadius = (newRadius: number) => {
    setData({ ...data, radius: Math.max(100, newRadius) });
  };

  const centerEventHandlers = useMemo(
    () => ({
      dragend() {
        const marker = markerRef.current;
        if (marker != null) {
          const { lat, lng } = marker.getLatLng();
          updateCoords(lat, lng);
        }
      },
    }),
    [data.coordinates]
  );

  const edgeEventHandlers = useMemo(
    () => ({
      drag(e: L.LeafletEvent) {
        const center = L.latLng(data.coordinates.lat, data.coordinates.lng);
        const draggedMarker = e.target as L.Marker;
        const newRadius = center.distanceTo(draggedMarker.getLatLng());
        updateRadius(Math.round(newRadius));
      },
    }),
    [data.coordinates]
  );

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1">
        <h2 className="text-2xl font-bold mb-4">Ustaw lokalizację i zasięg</h2>
        <div className="grid grid-cols-4 gap-4 mb-4">
          <div className="col-span-1">
            <label className="block text-sm font-medium mb-1">
              Promień (m)
            </label>
            <input
              type="number"
              value={data.radius}
              onChange={(e) => updateRadius(Number(e.target.value))}
              className="w-full px-3 py-2 border rounded-lg"
            />
          </div>
          <div className="col-span-3">
            <label className="block text-sm font-medium mb-1">
              Zasięg: {(data.radius / 1000).toFixed(1)} km
            </label>
            <input
              type="range"
              min="1000"
              max="100000"
              step="500"
              value={data.radius}
              onChange={(e) => updateRadius(Number(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
            />
          </div>
        </div>
        <div className="h-[400px] w-full rounded-xl overflow-hidden border relative">
          <MapContainer
            center={[data.coordinates.lat, data.coordinates.lng]}
            zoom={10}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <Marker
              draggable={true}
              eventHandlers={centerEventHandlers}
              position={[data.coordinates.lat, data.coordinates.lng]}
              ref={markerRef}
            />
            <Circle
              center={[data.coordinates.lat, data.coordinates.lng]}
              radius={data.radius}
              pathOptions={{ color: "#2563eb" }}
            />
            <Marker
              draggable={true}
              eventHandlers={edgeEventHandlers}
              position={[edgePoint.lat, edgePoint.lng]}
              icon={L.divIcon({
                className:
                  "bg-white border-2 border-blue-600 rounded-full w-4 h-4 shadow-md",
                iconSize: [16, 16],
              })}
            />
          </MapContainer>
        </div>
      </div>
      <div className="flex justify-between gap-4 pt-8 border-t mt-4">
        <button
          onClick={onPrev}
          className="px-6 py-2 bg-gray-300 text-gray-700 rounded-lg"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-6 py-2 bg-primary-600 text-white rounded-lg font-bold"
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
