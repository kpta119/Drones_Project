"use client";

import { useMemo, useRef, useCallback, useState, useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { getAddressFromCoordinates } from "../utils/geocoding";
import { FaMapPin, FaSearch } from "react-icons/fa";

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

function MapSync({ center }: { center: { lat: number; lng: number } }) {
  const map = useMap();
  useEffect(() => {
    map.flyTo([center.lat, center.lng], 14, { duration: 1.5 });
  }, [center, map]);
  return null;
}

interface OrderLocationPickerProps {
  onNext: () => void;
  onPrev: () => void;
  coordinates: { lat: number; lng: number };
  setCoordinates: (coords: { lat: number; lng: number }) => void;
}

export default function OrderLocationPicker({
  onNext,
  onPrev,
  coordinates,
  setCoordinates,
}: OrderLocationPickerProps) {
  const markerRef = useRef<L.Marker>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [displayAddress, setDisplayAddress] = useState(
    "Wybierz lokalizację..."
  );

  const syncTextAddress = useCallback(async (lat: number, lng: number) => {
    const addr = await getAddressFromCoordinates(`${lat},${lng}`);
    const fullAddr = `${addr.city}${addr.street ? ", " + addr.street : ""}`;
    setDisplayAddress(fullAddr);
    setSearchQuery(fullAddr);
  }, []);

  useEffect(() => {
    syncTextAddress(coordinates.lat, coordinates.lng);
  }, [coordinates.lat, coordinates.lng, syncTextAddress]);

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
          searchQuery
        )}&limit=1&addressdetails=1`
      );
      const data = await res.json();
      if (data && data.length > 0) {
        const newCoords = {
          lat: parseFloat(data[0].lat),
          lng: parseFloat(data[0].lon),
        };
        setCoordinates(newCoords);
      } else {
        alert("Błąd wyszukiwania adresu.");
      }
    } catch {
      console.error("Search failed");
    } finally {
      setIsSearching(false);
    }
  };

  const eventHandlers = useMemo(
    () => ({
      dragend() {
        const marker = markerRef.current;
        if (marker != null) {
          const { lat, lng } = marker.getLatLng();
          setCoordinates({ lat, lng });
        }
      },
    }),
    [setCoordinates]
  );

  return (
    <div className="flex flex-col h-full animate-fadeIn text-black font-montserrat">
      <div className="flex-1">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-primary-900 uppercase tracking-tight">
            Lokalizacja zlecenia
          </h2>
          <p className="text-gray-500 text-xs font-bold uppercase tracking-widest mt-1">
            Wyszukaj lub przesuń pinezkę.
          </p>
        </div>

        <div className="flex flex-col lg:flex-row gap-2 mb-6">
          <div className="relative flex-1">
            <FaSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-primary-700" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="Miasto, ulica..."
              className="w-full pl-11 pr-4 py-4 bg-gray-50 border-2 border-transparent focus:border-primary-300 focus:bg-white rounded-2xl outline-none font-bold text-black shadow-inner"
            />
          </div>
          <button
            onClick={handleSearch}
            disabled={isSearching}
            className="w-full lg:w-auto px-8 py-4 bg-primary-900 text-primary-50 rounded-2xl font-bold hover:bg-black transition-all disabled:opacity-50 shadow-lg uppercase text-xs tracking-widest"
          >
            Szukaj
          </button>
        </div>

        <div className="h-[280px] w-full rounded-[2.5rem] overflow-hidden border-4 border-primary-50 relative shadow-2xl mb-6">
          <MapContainer
            center={[coordinates.lat, coordinates.lng]}
            zoom={13}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <MapSync center={coordinates} />
            <Marker
              draggable={true}
              eventHandlers={eventHandlers}
              position={[coordinates.lat, coordinates.lng]}
              ref={markerRef}
            />
          </MapContainer>
        </div>

        <div className="bg-primary-100/50 p-3 rounded-3xl border-2 border-primary-200 mb-8 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-primary-400 rounded-2xl flex items-center justify-center text-lg shadow-md text-white shrink-0">
              <FaMapPin className="text-black"></FaMapPin>
            </div>
            <div>
              <p className="text-[9px] font-bold text-primary-800 uppercase tracking-[0.2em]">
                Punkt wykonania zlecenia:
              </p>
              <p className="text-sm font-bold text-primary-950 tracking-tight">
                {displayAddress}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between mb-7 items-center pt-6 border-t border-gray-100 mt-auto">
        <button
          onClick={onPrev}
          className="text-primary-800 font-bold uppercase tracking-widest text-xs hover:text-black"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-6 lg:px-12 py-2 lg:py-3 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-xl hover:bg-primary-400 transition-all uppercase tracking-widest text-xs lg:text-sm"
        >
          Potwierdź adres
        </button>
      </div>
    </div>
  );
}
