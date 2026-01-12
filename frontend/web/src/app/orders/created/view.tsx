"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import {
  OrderResponse,
  OperatorApplicantDto,
  OrderStatusLabels,
} from "../types";
import OpMatch from "./op_match";

// DEFINICJA PROPSÓW
interface CreatedViewProps {
  onCreateNew: () => void;
}

export default function CreatedView({ onCreateNew }: CreatedViewProps) {
  const [myOrders, setMyOrders] = useState<OrderResponse[]>([]);
  const [matchingOrderId, setMatchingOrderId] = useState<string | null>(null);

  useEffect(() => {
    const fetchMyOrders = async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch("/api/orders/getMyOrders", {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
        if (res.ok) setMyOrders(await res.json());
      } catch (err) {
        console.error(err);
      }
    };
    fetchMyOrders();
  }, []);

  const mockApplicant: OperatorApplicantDto = {
    id: "op-1",
    name: "Jan",
    surname: "Kowalski",
    username: "jkowal_drones",
    rating: 4,
    description: "Specjalizuję się w inspekcjach technicznych od 3 lat.",
  };

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-8">
      <div className="flex justify-center md:justify-end">
        <button
          onClick={onCreateNew} // UŻYCIE PROPSA
          className="flex items-center gap-3 px-10 py-4 bg-primary-500 text-black rounded-2xl font-bold shadow-lg hover:bg-primary-600 transition-all"
        >
          <span className="text-2xl">+</span>
          Wystaw nowe zlecenie
        </button>
      </div>

      <div className="space-y-6">
        {myOrders.length > 0 ? (
          myOrders.map((order) => (
            <div
              key={order.id}
              className="relative bg-slate-900 rounded-[2.5rem] p-8 text-white overflow-hidden shadow-xl"
            >
              <div className="absolute inset-0 opacity-20">
                <Image
                  src="/dron_zdj.png"
                  alt="bg"
                  fill
                  className="object-cover"
                />
              </div>
              <div className="relative z-10 flex flex-col md:flex-row justify-between items-center gap-6">
                <div className="text-center md:text-left">
                  <h3 className="text-2xl font-bold leading-tight">
                    {order.title}
                  </h3>
                  <p className="text-primary-500 font-medium">
                    {order.service}
                  </p>
                  <p className="text-gray-400 text-xs mt-2 uppercase tracking-widest font-bold">
                    {OrderStatusLabels[order.status]}
                  </p>
                </div>

                <div className="flex gap-3">
                  {order.status === "AWAITING_OPERATOR" && (
                    <button
                      onClick={() => setMatchingOrderId(order.id)}
                      className="px-8 py-3 bg-primary-500 text-black rounded-xl font-bold hover:bg-primary-600 transition-all shadow-lg active:scale-95"
                    >
                      Przejrzyj chętnych
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-20 text-gray-500">
            Brak wystawionych zleceń.
          </div>
        )}
      </div>

      {matchingOrderId && (
        <OpMatch
          applicant={mockApplicant}
          onClose={() => setMatchingOrderId(null)}
          onAccept={() => setMatchingOrderId(null)}
          onReject={() => setMatchingOrderId(null)}
        />
      )}
    </div>
  );
}
