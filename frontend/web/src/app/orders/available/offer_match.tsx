"use client";

import Image from "next/image";
import { useState } from "react";
import { MatchedOrderDto, OrderStatusLabels } from "../types";
import dynamic from "next/dynamic";
import { FaExternalLinkAlt } from "react-icons/fa";

const MapModule = dynamic(() => import("./map_module"), {
  ssr: false,
  loading: () => (
    <div className="fixed inset-0 z-100 bg-black/60 flex items-center justify-center text-white font-bold font-montserrat">
      Ładowanie mapy...
    </div>
  ),
});

interface OfferMatchProps {
  order: MatchedOrderDto;
  address: { city: string; street: string; country: string };
  isAddressLoading: boolean;
  onAccept: (id: string) => void;
  onReject: (id: string) => void;
}

export default function OfferMatch({
  order,
  address,
  isAddressLoading,
  onAccept,
  onReject,
}: OfferMatchProps) {
  const [showMap, setShowMap] = useState(false);

  const handleViewAuthor = () => {
    window.open(`/user_profile?user_id=${order.client_id}`, "_blank");
  };

  const displayStatus =
    OrderStatusLabels[order.order_status] || order.order_status;

  return (
    <div className="relative w-full max-w-7xl mt-12 bg-slate-900 rounded-[4rem] overflow-hidden shadow-2xl p-8 lg:p-10 text-white min-h-[420px] flex items-center font-montserrat animate-fadeIn">
      <div className="absolute inset-0 opacity-60">
        <Image src="/dron_zdj.png" alt="bg" fill className="object-cover" />
        <div className="absolute inset-0 bg-linear-to-r from-transparent via-slate-900/10 to-slate-900/90" />
      </div>

      <div className="relative z-10 flex flex-col lg:flex-row w-full gap-4 lg:gap-12 items-center text-white">
        <div className="hidden lg:block lg:w-1/2"></div>

        <div className="w-full lg:w-1/2 flex flex-col">
          <div className="text-right mb-4 lg:mb-6 min-h-[140px] flex flex-col justify-end">
            {isAddressLoading ? (
              <div className="space-y-2 animate-pulse flex flex-col items-end text-white text-right">
                <div className="h-4 w-20 bg-white/20 rounded"></div>
                <div className="h-12 w-64 bg-white/20 rounded"></div>
                <div className="h-6 w-48 bg-white/20 rounded"></div>
              </div>
            ) : (
              <div className="animate-fadeIn">
                <p className="text-white text-base uppercase tracking-widest font-medium">
                  {address.country}
                </p>
                <h2 className="text-5xl lg:text-7xl font-bold tracking-tighter leading-none text-white">
                  {address.city}
                </h2>
                <p className="text-white text-lg lg:text-xl font-light">
                  ul. {address.street}
                </p>
              </div>
            )}

            <div className="mt-2 flex flex-col items-end">
              <button
                onClick={handleViewAuthor}
                className="text-white/80 hover:text-white text-xs font-bold uppercase tracking-widest transition-colors flex items-center gap-2 mt-1 underline underline-offset-4"
              >
                <FaExternalLinkAlt size={10} />
                Zobacz profil autora
              </button>
            </div>
          </div>

          <div className="space-y-3 lg:space-y-4 text-left">
            <div>
              <h4 className="text-white/80 text-lg lg:text-xl font-medium">
                Temat:
              </h4>
              <p className="text-xl lg:text-2xl font-bold leading-tight tracking-tight text-white">
                {order.title}
              </p>
              <div className="text-white text-base lg:text-lg flex items-center gap-2">
                <span className="font-light opacity-70">Stan:</span>{" "}
                <span className="font-bold text-primary-300">
                  {displayStatus}
                </span>
              </div>
            </div>

            <div className="space-y-0">
              <p className="text-white text-base lg:text-lg">
                <span className="font-medium opacity-70">Serwis:</span>{" "}
                <span className="text-white font-bold">{order.service}</span>
              </p>
              <p className="text-white text-base leading-relaxed line-clamp-3">
                <span className="font-medium opacity-70">Opis:</span>{" "}
                {order.description}
              </p>
            </div>
          </div>

          <div className="mt-6 lg:mt-8 flex flex-col gap-3">
            <button
              onClick={() => setShowMap(true)}
              className="w-full py-2.5 bg-primary-900/40 hover:bg-primary-900/60 rounded-2xl transition-all font-bold text-base backdrop-blur-md border border-white/20 text-primary-100"
            >
              Zobacz na mapie
            </button>
            <div className="flex flex-col sm:flex-row gap-3 lg:gap-10">
              <button
                onClick={() => onReject(order.id)}
                className="w-full sm:flex-1 py-4 bg-red-600 hover:bg-red-700 rounded-2xl shadow-lg font-bold text-xl lg:text-2xl transition-all active:scale-95 text-white"
              >
                Zrezygnuj
              </button>
              <button
                onClick={() => onAccept(order.id)}
                className="w-full sm:flex-1 py-4 bg-green-500 hover:bg-green-600 rounded-2xl shadow-lg font-bold text-xl lg:text-2xl transition-all active:scale-95 text-white"
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
