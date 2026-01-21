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
  const [showFullDescription, setShowFullDescription] = useState(false);

  const handleViewAuthor = () => {
    window.open(`/user_profile?user_id=${order.client_id}`, "_blank");
  };

  const displayStatus =
    OrderStatusLabels[order.order_status] || order.order_status;

  const isDescriptionLong = order.description.length > 150;

  return (
    <div className="relative w-full max-w-6xl mt-4 md:mt-6 lg:mt-8 bg-slate-900 rounded-2xl md:rounded-3xl overflow-hidden shadow-2xl p-4 md:p-6 lg:p-8 text-white min-h-[280px] md:min-h-[320px] lg:min-h-[360px] flex items-center font-montserrat animate-fadeIn">
      <div className="absolute inset-0 opacity-60">
        <Image src="/dron_zdj.png" alt="bg" fill className="object-cover" />
        <div className="absolute inset-0 bg-linear-to-r from-transparent via-slate-900/10 to-slate-900/90" />
      </div>

      <div className="relative z-10 flex flex-col lg:flex-row w-full gap-2 md:gap-3 lg:gap-8 items-center text-white">
        <div className="hidden lg:block lg:w-1/2"></div>

        <div className="w-full lg:w-1/2 flex flex-col">
          <div className="text-right mb-2 md:mb-3 lg:mb-4 min-h-[70px] md:min-h-[85px] lg:min-h-[100px] flex flex-col justify-end">
            {isAddressLoading ? (
              <div className="space-y-2 animate-pulse flex flex-col items-end text-white text-right">
                <div className="h-4 w-20 bg-white/20 rounded"></div>
                <div className="h-12 w-64 bg-white/20 rounded"></div>
                <div className="h-6 w-48 bg-white/20 rounded"></div>
              </div>
            ) : (
              <div className="animate-fadeIn">
                <p className="text-white text-xs md:text-sm uppercase tracking-widest font-medium">
                  {address.country}
                </p>
                <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold tracking-tighter leading-none text-white">
                  {address.city}
                </h2>
                <p className="text-white text-sm md:text-base lg:text-lg font-light">
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

          <div className="space-y-1.5 md:space-y-2 lg:space-y-3 text-left">
            <div>
              <h4 className="text-white/80 text-sm md:text-base lg:text-lg font-medium">
                Temat:
              </h4>
              <p className="text-base md:text-lg lg:text-xl font-bold leading-tight tracking-tight text-white">
                {order.title}
              </p>
              <div className="text-white text-sm md:text-base lg:text-lg flex items-center gap-2">
                <span className="font-light opacity-70">Stan:</span>{" "}
                <span className="font-bold text-primary-300">
                  {displayStatus}
                </span>
              </div>
            </div>

            <div className="space-y-0">
              <p className="text-white text-sm md:text-base lg:text-lg">
                <span className="font-medium opacity-70">Serwis:</span>{" "}
                <span className="text-white font-bold">{order.service}</span>
              </p>
              <div>
                <p className="text-white text-sm md:text-base leading-relaxed line-clamp-3">
                  <span className="font-medium opacity-70">Opis:</span>{" "}
                  {order.description}
                </p>
                {isDescriptionLong && (
                  <button
                    onClick={() => setShowFullDescription(true)}
                    className="text-primary-300 hover:text-primary-200 text-xs md:text-sm font-medium underline mt-1 transition-colors"
                  >
                    Zobacz pełny opis
                  </button>
                )}
              </div>
            </div>

            {order.parameters && Object.keys(order.parameters).length > 0 && (
              <div className="space-y-1 md:space-y-1.5">
                <h4 className="text-white/80 text-sm md:text-base lg:text-lg font-medium">
                  Parametry:
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1 md:gap-1.5">
                  {Object.entries(order.parameters).map(([key, value]) => (
                    <div
                      key={key}
                      className="bg-white/10 backdrop-blur-sm rounded-md md:rounded-lg px-2 md:px-2.5 py-1 md:py-1.5 border border-white/20"
                    >
                      <span className="text-white/70 text-[10px] md:text-xs font-medium">
                        {key}:
                      </span>{" "}
                      <span className="text-white font-bold">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="mt-3 md:mt-4 lg:mt-6 flex flex-col gap-1.5 md:gap-2">
            <button
              onClick={() => setShowMap(true)}
              className="w-full py-1.5 md:py-2 bg-primary-900/40 hover:bg-primary-900/60 rounded-lg md:rounded-xl transition-all font-bold text-xs md:text-sm backdrop-blur-md border border-white/20 text-primary-100"
            >
              Zobacz na mapie
            </button>
            <div className="flex flex-col sm:flex-row gap-1.5 md:gap-2 lg:gap-4">
              <button
                onClick={() => onReject(order.id)}
                className="w-full sm:flex-1 py-2.5 md:py-3 bg-red-600 hover:bg-red-700 rounded-lg md:rounded-xl shadow-lg font-bold text-base md:text-lg lg:text-xl transition-all active:scale-95 text-white"
              >
                Zrezygnuj
              </button>
              <button
                onClick={() => onAccept(order.id)}
                className="w-full sm:flex-1 py-2.5 md:py-3 bg-green-500 hover:bg-green-600 rounded-lg md:rounded-xl shadow-lg font-bold text-base md:text-lg lg:text-xl transition-all active:scale-95 text-white"
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

      {showFullDescription && (
        <div
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 font-montserrat"
          onClick={() => setShowFullDescription(false)}
        >
          <div
            className="bg-slate-800 rounded-2xl p-6 md:p-8 max-w-2xl w-full max-h-[80vh] overflow-y-auto shadow-2xl border border-white/20"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex justify-between items-start mb-4">
              <h3 className="text-xl md:text-2xl font-bold text-white">
                Pełny opis zlecenia
              </h3>
              <button
                onClick={() => setShowFullDescription(false)}
                className="text-white/60 hover:text-white text-2xl font-bold transition-colors"
              >
                ×
              </button>
            </div>
            <div className="text-white text-sm md:text-base leading-relaxed whitespace-pre-wrap">
              {order.description}
            </div>
            <button
              onClick={() => setShowFullDescription(false)}
              className="mt-6 w-full py-2.5 bg-primary-600 hover:bg-primary-700 rounded-xl font-bold text-white transition-all"
            >
              Zamknij
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
