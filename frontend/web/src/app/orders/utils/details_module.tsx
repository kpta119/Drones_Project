"use client";

import { OrderResponse, OrderStatusLabels } from "../types";
import {
  FaTimes,
  FaMapMarkerAlt,
  FaCalendarAlt,
  FaUser,
  FaInfoCircle,
} from "react-icons/fa";
import { useEffect, useState } from "react";
import { getAddressFromCoordinates } from "./geocoding";
import SpecificationsDisplay from "./specifications_display";

interface OrderDetailsModuleProps {
  order: OrderResponse;
  onClose: () => void;
  assignedOperatorId?: string;
}

export default function OrderDetailsModule({
  order,
  onClose,
  assignedOperatorId,
}: OrderDetailsModuleProps) {
  const [address, setAddress] = useState({ city: "", street: "", country: "" });

  useEffect(() => {
    getAddressFromCoordinates(order.coordinates).then(setAddress);
  }, [order.coordinates]);

  const handleViewProfile = (id: string) => {
    window.open(`/user_profile?user_id=${id}`, "_blank");
  };

  return (
    <div className="fixed inset-0 z-9999 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn font-montserrat  text-black">
      <div className="bg-white w-full max-w-2xl rounded-[3rem] overflow-hidden shadow-2xl relative border border-white/20">
        <button
          onClick={onClose}
          className="absolute top-8 right-8 text-gray-400 hover:text-black transition-colors z-10"
        >
          <FaTimes size={24} />
        </button>

        <div className="p-8 lg:p-12">
          <div className="mb-8">
            <span className="bg-primary-100 text-primary-700 px-4 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-widest border border-primary-200 inline-block mb-4">
              {OrderStatusLabels[order.status]}
            </span>
            <h2 className="text-4xl font-bold text-gray-900 tracking-tight leading-none">
              {order.title}
            </h2>
            <p className="text-primary-600 font-bold text-sm uppercase tracking-widest mt-2">
              {order.service}
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
            <div className="flex items-center gap-4 bg-gray-50 p-5 rounded-3xl border border-gray-100">
              <div className="w-10 h-10 bg-primary-100 rounded-xl flex items-center justify-center text-primary-600">
                <FaMapMarkerAlt />
              </div>
              <div>
                <p className="text-[9px] font-bold text-gray-400 uppercase tracking-widest">
                  Lokalizacja
                </p>
                <p className="text-sm font-bold text-gray-800">
                  {address.city || "Pobieranie..."}
                </p>
                <p className="text-[10px] text-gray-500">{address.street}</p>
              </div>
            </div>
            <div className="flex items-center gap-4 bg-gray-50 p-5 rounded-3xl border border-gray-100">
              <div className="w-10 h-10 bg-primary-100 rounded-xl flex items-center justify-center text-primary-600">
                <FaCalendarAlt />
              </div>
              <div>
                <p className="text-[9px] font-bold text-gray-400 uppercase tracking-widest">
                  Termin
                </p>
                <p className="text-sm font-bold text-gray-800">
                  {order.from_date.split("T")[0]}
                </p>
                <p className="text-[10px] text-gray-500">
                  Godzina: {order.from_date.split("T")[1]?.substring(0, 5)}
                </p>
              </div>
            </div>
          </div>

          <div className="mb-10">
            <div className="flex items-center gap-2 mb-4 text-primary-900">
              <FaInfoCircle />
              <h4 className="font-bold uppercase text-[10px] tracking-[0.2em]">
                Dokumentacja i opis
              </h4>
            </div>
            <div className="text-gray-600 text-sm leading-relaxed bg-gray-50 p-6 rounded-2rem border border-gray-100 min-h-[100px] italic">
              {order.description || "Brak dodatkowego opisu do tego zlecenia."}
            </div>
          </div>

          <SpecificationsDisplay specifications={order.parameters} />

          <div className="flex flex-col sm:flex-row gap-4">
            {assignedOperatorId && (
              <button
                onClick={() => handleViewProfile(assignedOperatorId)}
                className="flex-1 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-lg hover:bg-primary-400 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-[10px]"
              >
                <FaUser />
                Sprawdź operatora
              </button>
            )}
            <button
              onClick={onClose}
              className="flex-1 py-4 bg-gray-100 text-gray-500 rounded-2xl font-bold hover:bg-gray-200 transition-all uppercase tracking-widest text-[10px]"
            >
              Zamknij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
