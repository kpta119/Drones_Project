"use client";

import { useEffect, useState } from "react";
import OfferMatch from "./offer_match";
import { MatchedOrderDto } from "../types";
import { getAddressFromCoordinates } from "../utils/geocoding";

export default function AvailableView({ isOperator }: { isOperator: boolean }) {
  const [matches, setMatches] = useState<MatchedOrderDto[]>([]);
  const [address, setAddress] = useState({ city: "", street: "", country: "" });
  const [loading, setLoading] = useState(true);
  const [addressLoading, setAddressLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (!isOperator) {
      setLoading(false);
      return;
    }

    const fetchAvailable = async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch("/api/operators/getMatchedOrders", {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setMatches(data.content || []);
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchAvailable();
  }, [isOperator]);

  useEffect(() => {
    if (matches[currentIndex]) {
      setAddressLoading(true);
      getAddressFromCoordinates(matches[currentIndex].coordinates).then(
        (addr) => {
          setAddress(addr);
          setAddressLoading(false);
        }
      );
    }
  }, [currentIndex, matches]);

  const handleNext = () => {
    setCurrentIndex((prev) => prev + 1);
  };

  if (!isOperator) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center animate-fadeIn text-black font-montserrat">
        <div className="w-24 h-24 bg-primary-100 rounded-full flex items-center justify-center text-4xl mb-8 shadow-inner border border-primary-200">
          🔒
        </div>
        <h2 className="text-3xl font-bold mb-4 text-primary-900 uppercase tracking-tight">
          Sekcja tylko dla operatorów
        </h2>
        <p className="text-gray-500 max-w-md font-medium leading-relaxed px-4">
          Musisz posiadać zweryfikowany profil operatora, aby móc przeglądać
          dostępne zlecenia i zarabiać na lotach.
        </p>
        <button
          onClick={() => (window.location.href = "/user_profile")}
          className="mt-10 px-12 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold hover:bg-primary-400 transition-all shadow-xl uppercase tracking-widest text-sm"
        >
          Załóż profil operatora
        </button>
      </div>
    );
  }

  if (loading)
    return (
      <div className="text-primary-800 font-bold py-20 text-center animate-pulse">
        Szukanie zleceń...
      </div>
    );
  if (currentIndex >= matches.length)
    return (
      <div className="text-gray-400 font-light py-20 text-center">
        Brak nowych ofert w Twoim zasięgu.
      </div>
    );

  return (
    <div className="w-full flex justify-center overflow-hidden">
      <OfferMatch
        key={matches[currentIndex].id}
        order={matches[currentIndex]}
        address={address}
        isAddressLoading={addressLoading}
        onAccept={handleNext}
        onReject={handleNext}
      />
    </div>
  );
}
