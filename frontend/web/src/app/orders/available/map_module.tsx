"use client";

import { MapContainer, TileLayer, Marker } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

const DefaultIcon = L.icon({
  iconUrl:
    (markerIcon as unknown as { src: string }).src ||
    (markerIcon as unknown as string),
  shadowUrl:
    (markerShadow as unknown as { src: string }).src ||
    (markerShadow as unknown as string),
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});
L.Marker.prototype.options.icon = DefaultIcon;

interface MapModuleProps {
  coords: string;
  onClose: () => void;
}

export default function MapModule({ coords, onClose }: MapModuleProps) {
  const [lat, lng] = coords.split(",").map((c) => parseFloat(c.trim()));

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="relative w-full max-w-4xl h-[70vh] bg-white rounded-2rem overflow-hidden shadow-2xl">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-1001 bg-white w-10 h-10 rounded-full flex items-center justify-center text-black font-bold shadow-lg hover:bg-gray-100 transition-colors"
        >
          ✕
        </button>
        <MapContainer
          center={[lat, lng]}
          zoom={13}
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <Marker position={[lat, lng]} />
        </MapContainer>
      </div>
    </div>
  );
}
