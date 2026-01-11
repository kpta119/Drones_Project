"use client";

import Image from "next/image";
import { useState } from "react";
import { MatchedOrderDto, OrderStatusLabels } from "../types";
import dynamic from "next/dynamic";

const MapModule = dynamic(() => import("./map_module"), {
  ssr: false,
  loading: () => (
    <div className="fixed inset-0 z-100 bg-black/60 flex items-center justify-center text-white">
      Ładowanie mapy...
    </div>
  ),
});

interface OfferMatchProps {
  order: MatchedOrderDto;
  address: { city: string; street: string };
  onAccept: (id: string) => void;
  onReject: (id: string) => void;
}

export default function OfferMatch({
  order,
  address,
  onAccept,
  onReject,
}: OfferMatchProps) {
  const [showMap, setShowMap] = useState(false);

  return (
    <div className="relative w-full max-w-5xl bg-slate-900 rounded-[3rem] overflow-hidden shadow-2xl p-6 lg:p-10 text-white min-h-[500px]">
      <div className="absolute inset-0 opacity-40">
        <Image src="/dron_zdj.png" alt="bg" fill className="object-cover" />
      </div>

      <div className="relative z-10 flex flex-col lg:flex-row gap-10">
        <div className="w-full lg:w-1/2 flex items-center justify-center bg-white/5 rounded-[2.5rem] border border-white/10 min-h-[250px]">
          <div className="text-center">
            <span className="text-8xl">🛸</span>
            <p className="mt-4 text-primary-500 font-bold uppercase tracking-widest">
              {order.service}
            </p>
          </div>
        </div>

        <div className="w-full lg:w-1/2 flex flex-col justify-between">
          <div className="text-right">
            <h2 className="text-4xl lg:text-5xl font-bold leading-tight">
              {address.city}
            </h2>
            <p className="text-gray-400">{address.street}</p>
            <p className="text-primary-400 text-sm mt-1 font-medium italic">
              Odległość: {(order.distance / 1000).toFixed(1)} km
            </p>
          </div>

          <div className="mt-6 space-y-4">
            <div>
              <h3 className="text-2xl font-bold leading-tight mb-1">
                {order.title}
              </h3>
              <p className="text-yellow-400 text-xs font-bold uppercase tracking-tighter">
                Status: {OrderStatusLabels[order.status]}
              </p>
            </div>
            <p className="text-gray-300 text-sm line-clamp-6 leading-relaxed">
              {order.description}
            </p>
          </div>

          <div className="mt-10 space-y-4">
            <button
              onClick={() => setShowMap(true)}
              className="w-full py-3 bg-white/10 hover:bg-white/20 rounded-2xl transition-all font-semibold border border-white/5"
            >
              Zobacz na mapie
            </button>
            <div className="flex gap-4">
              <button
                onClick={() => onReject(order.id)}
                className="flex-1 py-4 bg-red-600 hover:bg-red-700 rounded-2xl shadow-lg font-bold text-xl transition-all active:scale-95"
              >
                Zrezygnuj
              </button>
              <button
                onClick={() => onAccept(order.id)}
                className="flex-1 py-4 bg-green-500 hover:bg-green-600 rounded-2xl shadow-lg font-bold text-xl transition-all active:scale-95"
              >
                Akceptuj
              </button>
            </div>
          </div>
        </div>
      </div>

      {showMap && (
        <MapModule
          coords={order.coordinates}
          onClose={() => setShowMap(false)}
        />
      )}
    </div>
  );
}
