"use client";

import { useMemo, useRef, useCallback, useState, useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { getAddressFromCoordinates } from "../utils/geocoding";

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

// Komponent pomocniczy do przesuwania widoku mapy po wyszukiwaniu
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

  // Funkcja aktualizująca adres tekstowy na bazie współrzędnych (Geocoding wsteczny)
  const syncTextAddress = useCallback(async (lat: number, lng: number) => {
    const addr = await getAddressFromCoordinates(`${lat},${lng}`);
    const fullAddr = `${addr.city}${addr.street ? ", " + addr.street : ""}`;
    setDisplayAddress(fullAddr);
    setSearchQuery(fullAddr); // Aktualizujemy input, żeby pasował do pinezki
  }, []);

  // Inicjalizacja adresu przy starcie
  useEffect(() => {
    syncTextAddress(coordinates.lat, coordinates.lng);
  }, [coordinates.lat, coordinates.lng, syncTextAddress]);

  // Funkcja wyszukiwania współrzędnych na bazie tekstu (Geocoding)
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
        // syncTextAddress zostanie wywołane przez useEffect powyżej
      } else {
        alert("Nie znaleziono takiego adresu.");
      }
    } catch (err) {
      console.error("Search error:", err);
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
          // Po przeciągnięciu, syncTextAddress zaktualizuje input tekstowy
        }
      },
    }),
    [setCoordinates]
  );

  return (
    <div className="flex flex-col h-full animate-fadeIn text-black font-montserrat">
      <div className="flex-1">
        <div className="mb-6">
          <h2 className="text-2xl font-bold">Gdzie wykonasz zlecenie?</h2>
          <p className="text-gray-500 text-sm">
            Wpisz miasto/adres lub przesuń pinezkę.
          </p>
        </div>

        {/* WYSZUKIWARKA */}
        <div className="flex gap-2 mb-6">
          <div className="relative flex-1">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
              🔍
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              placeholder="Wyszukaj miasto lub ulicę..."
              className="w-full pl-11 pr-4 py-4 bg-gray-50 border-2 border-gray-100 rounded-2xl focus:border-primary-500 outline-none transition-all font-medium text-black"
            />
          </div>
          <button
            onClick={handleSearch}
            disabled={isSearching}
            className="px-8 py-4 bg-gray-900 text-white rounded-2xl font-bold hover:bg-black transition-all disabled:opacity-50 shadow-lg"
          >
            {isSearching ? "..." : "Szukaj"}
          </button>
        </div>

        {/* MAPA */}
        <div className="h-[350px] w-full rounded-[2.5rem] overflow-hidden border-2 border-gray-100 relative shadow-inner mb-6">
          <MapContainer
            center={[coordinates.lat, coordinates.lng]}
            zoom={13}
            style={{ height: "100%", width: "100%" }}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

            {/* Ten komponent przesuwa mapę po wyszukiwaniu */}
            <MapSync center={coordinates} />

            <Marker
              draggable={true}
              eventHandlers={eventHandlers}
              position={[coordinates.lat, coordinates.lng]}
              ref={markerRef}
            />
          </MapContainer>
        </div>

        {/* WYBRANY ADRES - FEEDBACK */}
        <div className="bg-primary-50 p-5 rounded-3xl border border-primary-100 mb-8">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 bg-primary-500 rounded-2xl flex items-center justify-center text-xl shadow-md">
              📍
            </div>
            <div>
              <p className="text-[10px] font-extrabold text-primary-700 uppercase tracking-[0.2em]">
                Miejsce operacji:
              </p>
              <p className="text-base font-bold text-gray-900">
                {displayAddress}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex justify-between items-center pt-6 border-t border-gray-100">
        <button
          onClick={onPrev}
          className="px-8 py-2 text-gray-400 font-bold hover:text-black transition-colors"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-12 py-4 bg-primary-600 text-white rounded-2xl font-bold shadow-xl hover:bg-primary-700 hover:scale-[1.02] transition-all"
        >
          Potwierdź to miejsce
        </button>
      </div>
    </div>
  );
}
