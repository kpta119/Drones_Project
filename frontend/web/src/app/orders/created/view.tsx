"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import {
  OrderResponse,
  OperatorApplicantDto,
  OrderStatusLabels,
} from "../types";
import OpMatch from "./op_match";
import OrderDetailsModule from "../utils/details_module";
import {
  FaTrash,
  FaEdit,
  FaPlus,
  FaSearchPlus,
  FaUserTie,
} from "react-icons/fa";

interface CreatedViewProps {
  onCreateNew: () => void;
  onEdit: (order: OrderResponse) => void;
}

export default function CreatedView({ onCreateNew, onEdit }: CreatedViewProps) {
  const [myOrders, setMyOrders] = useState<OrderResponse[]>([]);
  const [matchingOrderId, setMatchingOrderId] = useState<string | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<OrderResponse | null>(
    null
  );

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

  useEffect(() => {
    fetchMyOrders();
  }, []);

  const handleCancelOrder = async (id: string) => {
    if (!confirm("Czy na pewno chcesz anulować to zlecenie?")) return;
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/orders/cancelOrder/${id}`, {
        method: "PATCH",
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      if (res.ok) fetchMyOrders();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-8 font-montserrat text-black">
      <div className="flex justify-center md:justify-end">
        <button
          onClick={onCreateNew}
          className="flex items-center gap-3 px-10 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-lg hover:bg-primary-400 transition-all border-2 border-primary-500/20 uppercase tracking-widest text-sm"
        >
          <FaPlus />
          Wystaw nowe zlecenie
        </button>
      </div>

      <div className="space-y-6">
        {myOrders.map((order) => (
          <div
            key={order.id}
            className="relative bg-slate-900 rounded-[2.5rem] p-8 text-white overflow-hidden shadow-xl border border-white/5"
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
                <p className="text-primary-300 font-bold text-lg">
                  {order.service}
                </p>
                <p className="text-gray-400 text-xs mt-2 uppercase tracking-widest font-bold">
                  Status:{" "}
                  <span className="text-primary-200">
                    {OrderStatusLabels[order.status]}
                  </span>
                </p>
              </div>

              <div className="flex flex-wrap justify-center items-center gap-3">
                <div
                  onClick={() => setSelectedOrder(order)}
                  className="group flex items-center flex-row bg-white/10 rounded-xl hover:bg-primary-500 hover:text-black transition-all cursor-pointer overflow-hidden"
                >
                  <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest pl-0 group-hover:pl-4">
                    Szczegóły
                  </span>
                  <div className="p-4">
                    <FaSearchPlus />
                  </div>
                </div>

                {(order.status === "IN_PROGRESS" ||
                  order.status === "COMPLETED") &&
                  (order as any).assigned_operator_id && (
                    <div
                      onClick={() =>
                        window.open(
                          `/user_profile?user_id=${
                            (order as any).assigned_operator_id
                          }`,
                          "_blank"
                        )
                      }
                      className="group flex items-center flex-row bg-primary-400 text-black rounded-xl hover:bg-primary-300 transition-all cursor-pointer overflow-hidden"
                    >
                      <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest pl-0 group-hover:pl-4">
                        Operator
                      </span>
                      <div className="p-4">
                        <FaUserTie />
                      </div>
                    </div>
                  )}

                {order.status === "AWAITING_OPERATOR" && (
                  <button
                    onClick={() => setMatchingOrderId(order.id)}
                    className="px-6 py-4 bg-primary-300 text-primary-900 rounded-xl font-bold hover:bg-primary-400 transition-all active:scale-95 text-xs uppercase"
                  >
                    Chętni
                  </button>
                )}

                {order.status === "OPEN" && (
                  <div
                    onClick={() => onEdit(order)}
                    className="group flex items-center flex-row bg-white/10 rounded-xl hover:bg-primary-500 hover:text-black transition-all cursor-pointer overflow-hidden"
                  >
                    <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest pl-0 group-hover:pl-4">
                      Edytuj
                    </span>
                    <div className="p-4">
                      <FaEdit />
                    </div>
                  </div>
                )}

                {order.status !== "COMPLETED" &&
                  order.status !== "CANCELLED" && (
                    <div
                      onClick={() => handleCancelOrder(order.id)}
                      className="group flex items-center flex-row bg-white/10 rounded-xl hover:bg-red-600 transition-all cursor-pointer overflow-hidden"
                    >
                      <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest pl-0 group-hover:pl-4">
                        Anuluj
                      </span>
                      <div className="p-4">
                        <FaTrash />
                      </div>
                    </div>
                  )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {selectedOrder && (
        <OrderDetailsModule
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          assignedOperatorId={(selectedOrder as any).assigned_operator_id}
        />
      )}
      {matchingOrderId && (
        <OpMatch
          applicant={{
            id: "1",
            name: "Jan",
            surname: "Kowalski",
            username: "jan_drone",
            rating: 5,
            description: "Test",
          }}
          onClose={() => setMatchingOrderId(null)}
          onAccept={() => setMatchingOrderId(null)}
          onReject={() => setMatchingOrderId(null)}
        />
      )}
    </div>
  );
}
