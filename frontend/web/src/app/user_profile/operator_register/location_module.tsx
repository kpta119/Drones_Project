"use client";

import { useMemo, useRef, useCallback } from "react";
import { MapContainer, TileLayer, Marker, Circle } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { RegistrationData } from "./operator_register_module";

import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

interface LeafletImage {
  src: string;
}

if (typeof window !== "undefined") {
  const iconUrl =
    (markerIcon as unknown as LeafletImage).src ||
    (markerIcon as unknown as string);
  const shadowUrl =
    (markerShadow as unknown as LeafletImage).src ||
    (markerShadow as unknown as string);

  const DefaultIcon = L.icon({
    iconUrl,
    shadowUrl,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
  });
  L.Marker.prototype.options.icon = DefaultIcon;
}

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

  const updateCoords = useCallback(
    (lat: number, lng: number) => {
      setData({ ...data, coordinates: { lat, lng } });
    },
    [data, setData]
  );

  const updateRadius = (newRadius: number) => {
    setData({ ...data, radius: newRadius });
  };

  const handleRadiusBlur = () => {
    if (data.radius < 1 || isNaN(data.radius)) {
      setData({ ...data, radius: 1 });
    }
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
    [updateCoords]
  );

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1">
        <h2 className="text-2xl font-bold mb-4">Ustaw lokalizację i zasięg</h2>
        <div className="grid grid-cols-4 gap-4 mb-4">
          <div className="col-span-1">
            <label className="block text-sm font-medium mb-1">
              Promień (km)
            </label>
            <input
              type="number"
              value={data.radius || ''}
              onChange={(e) => updateRadius(e.target.value === '' ? 0 : Number(e.target.value))}
              onBlur={handleRadiusBlur}
              step="0.1"
              min="1"
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
            />
          </div>
          <div className="col-span-3">
            <label className="block text-sm font-medium mb-1">
              Zasięg: {data.radius.toFixed(1)} km
            </label>
            <input
              type="range"
              min="1"
              max="300"
              step="0.5"
              value={data.radius}
              onChange={(e) => updateRadius(Number(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600 mt-3"
            />
          </div>
        </div>
        <div className="h-[400px] w-full rounded-xl overflow-hidden border relative shadow-inner">
          <MapContainer
            center={[data.coordinates.lat, data.coordinates.lng]}
            zoom={10}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <Marker
              draggable={true}
              eventHandlers={centerEventHandlers}
              position={[data.coordinates.lat, data.coordinates.lng]}
              ref={markerRef}
            />
            <Circle
              center={[data.coordinates.lat, data.coordinates.lng]}
              radius={data.radius * 1000}
              pathOptions={{
                color: "#2563eb",
                fillColor: "#2563eb",
                fillOpacity: 0.15,
                weight: 2,
              }}
            />
          </MapContainer>
          <div className="absolute bottom-2 left-2 z-1000 bg-white/80 backdrop-blur-sm px-3 py-1 text-xs rounded border shadow-sm pointer-events-none">
            Aby zmienić lokalizację, przesuń pinezkę.
          </div>
        </div>
      </div>
      <div className="flex justify-between gap-4 pt-8 border-t mt-4">
        <button
          onClick={onPrev}
          className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-6 py-2 bg-primary-600 text-white rounded-lg font-bold hover:bg-primary-700 transition-colors shadow-md"
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
