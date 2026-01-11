"use client";

import { useEffect, useState } from "react";
import OfferMatch from "./offer_match";
import { MatchedOrderDto } from "../types";
import { getAddressFromCoordinates } from "../utils/geocoding";

export default function AvailableView() {
  const [matches, setMatches] = useState<MatchedOrderDto[]>([]);
  const [address, setAddress] = useState({ city: "", street: "" });
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);

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

  useEffect(() => {
    fetchAvailable();
  }, []);

  useEffect(() => {
    if (matches[currentIndex]) {
      getAddressFromCoordinates(matches[currentIndex].coordinates).then(
        setAddress
      );
    }
  }, [currentIndex, matches]);

  const handleDecision = async (id: string, action: "accept" | "reject") => {
    try {
      const token = localStorage.getItem("token");
      const endpoint = action === "accept" ? "acceptOrder" : "rejectOrder";

      const res = await fetch(`/api/orders/${endpoint}/${id}`, {
        method: "PATCH",
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });

      if (res.ok) {
        setCurrentIndex((prev) => prev + 1);
      }
    } catch (err) {
      console.error(err);
    }
  };

  if (loading)
    return (
      <div className="text-gray-500 font-medium py-20 text-center">
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
    <div className="w-full flex justify-center animate-fadeIn">
      <OfferMatch
        order={matches[currentIndex]}
        address={address}
        onAccept={(id) => handleDecision(id, "accept")}
        onReject={(id) => handleDecision(id, "reject")}
      />
    </div>
  );
}
