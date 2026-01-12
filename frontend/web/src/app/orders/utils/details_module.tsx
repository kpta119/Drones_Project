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
    <div className="fixed inset-0 z-100 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn font-montserrat text-black">
      <div className="bg-white w-full max-w-2xl rounded-[2.5rem] overflow-hidden shadow-2xl relative animate-scaleIn">
        <button
          onClick={onClose}
          className="absolute top-6 right-6 text-gray-400 hover:text-black transition-colors z-10"
        >
          <FaTimes size={24} />
        </button>

        <div className="p-8 lg:p-12">
          <div className="flex items-start justify-between mb-8">
            <div>
              <span className="bg-primary-100 text-primary-700 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest border border-primary-200">
                {OrderStatusLabels[order.status]}
              </span>
              <h2 className="text-3xl font-bold text-gray-900 mt-2 tracking-tight">
                {order.title}
              </h2>
              <p className="text-primary-600 font-bold text-sm uppercase tracking-wide mt-1">
                {order.service}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
            <div className="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl">
              <FaMapMarkerAlt className="text-primary-500 text-xl" />
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase">
                  Lokalizacja
                </p>
                <p className="text-sm font-bold">
                  {address.city}, {address.street}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl">
              <FaCalendarAlt className="text-primary-500 text-xl" />
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase">
                  Termin realizacji
                </p>
                <p className="text-sm font-bold">
                  {order.from_date.split("T")[0]}
                </p>
              </div>
            </div>
          </div>

          <div className="mb-8">
            <div className="flex items-center gap-2 mb-3 text-primary-900">
              <FaInfoCircle />
              <h4 className="font-bold uppercase text-xs tracking-widest">
                Szczegóły zlecenia
              </h4>
            </div>
            <p className="text-gray-600 text-sm leading-relaxed bg-gray-50 p-6 rounded-3xl border border-gray-100">
              {order.description}
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-4">
            {assignedOperatorId && (
              <button
                onClick={() => handleViewProfile(assignedOperatorId)}
                className="flex-1 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-lg hover:bg-primary-400 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-xs"
              >
                <FaUser />
                Sprawdź operatora
              </button>
            )}
            <button
              onClick={onClose}
              className="flex-1 py-4 bg-gray-100 text-gray-500 rounded-2xl font-bold hover:bg-gray-200 transition-all uppercase tracking-widest text-xs"
            >
              Zamknij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
