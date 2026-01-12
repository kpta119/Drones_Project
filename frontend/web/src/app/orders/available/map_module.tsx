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
    <div className="fixed inset-0 z-9999 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="relative w-full max-w-5xl h-[80vh] bg-white rounded-[3rem] overflow-hidden shadow-2xl border-4 border-white/20">
        <button
          onClick={onClose}
          className="absolute top-6 right-6 z-10000 bg-white w-12 h-12 rounded-full flex items-center justify-center text-black font-bold shadow-2xl hover:bg-gray-100 transition-all hover:scale-110 active:scale-95"
        >
          ✕
        </button>
        <MapContainer
          center={[lat, lng]}
          zoom={15}
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <Marker position={[lat, lng]} />
        </MapContainer>
      </div>
    </div>
  );
}
