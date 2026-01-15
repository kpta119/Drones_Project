"use client";

import { useEffect, useState, useCallback } from "react";
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
  FaCheckCircle,
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
  const [applicants, setApplicants] = useState<OperatorApplicantDto[]>([]);
  const [currentAppIndex, setCurrentAppIndex] = useState(0);

  const fetchMyOrders = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch("/api/orders/getMyOrders", {
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        const filtered = data.filter(
          (order: OrderResponse) =>
            order.status !== "COMPLETED" && order.status !== "CANCELLED"
        );
        setMyOrders(filtered);
      }
    } catch (err) {
      console.error(err);
    }
  }, []);

  useEffect(() => {
    fetchMyOrders();
  }, [fetchMyOrders]);

  const fetchApplicants = async (orderId: string) => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/operators/getOperatorsInfo/${orderId}`, {
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        const list = Array.isArray(data) ? data : [];
        if (list.length > 0) {
          setApplicants(list);
          setCurrentAppIndex(0);
          setMatchingOrderId(orderId);
        } else {
          alert("Brak chętnych operatorów dla tego zlecenia.");
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleOpDecision = async (
    operatorId: string,
    action: "accept" | "reject"
  ) => {
    if (!matchingOrderId || !operatorId) return;
    try {
      const token = localStorage.getItem("token");
      const endpoint = action === "accept" ? "acceptOrder" : "rejectOrder";
      const res = await fetch(
        `/api/orders/${endpoint}/${matchingOrderId}?operatorId=${operatorId}`,
        {
          method: "PATCH",
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        }
      );
      if (res.ok) {
        if (currentAppIndex < applicants.length - 1) {
          setCurrentAppIndex((prev) => prev + 1);
        } else {
          setMatchingOrderId(null);
          fetchMyOrders();
        }
      }
    } catch (err) {
      console.error(err);
    }
  };

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

  const handleFinishOrder = async (id: string) => {
    if (!confirm("Czy na pewno chcesz zakończyć to zlecenie?")) return;
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/orders/finishOrder/${id}`, {
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
            className="relative bg-slate-900 rounded-[2.5rem] p-8 overflow-hidden shadow-xl border border-white/5 text-white"
          >
            <div className="absolute inset-0 opacity-20">
              <Image
                src="/dron_zdj.png"
                alt="bg"
                fill
                className="object-cover"
              />
            </div>
            <div className="relative z-10 flex flex-col md:flex-row justify-between items-center gap-6 text-white">
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
                {(order.status === "IN_PROGRESS" ||
                  order.status === "COMPLETED") &&
                  (order as any).operator_id && (
                    <div
                      onClick={() =>
                        window.open(
                          `/user_profile?user_id=${(order as any).operator_id}`,
                          "_blank"
                        )
                      }
                      className="group flex items-center bg-primary-300 rounded-xl hover:bg-primary-400 transition-all cursor-pointer overflow-hidden h-10 shadow-sm"
                    >
                      <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-primary-950 pl-0 group-hover:pl-4">
                        Wykonawca
                      </span>
                      <div className="p-3 text-primary-950">
                        <FaUserTie size={14} />
                      </div>
                    </div>
                  )}

                <div
                  onClick={() => setSelectedOrder(order)}
                  className="group flex items-center bg-white/10 rounded-xl hover:bg-white/20 transition-all cursor-pointer overflow-hidden h-10"
                >
                  <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-white pl-0 group-hover:pl-4">
                    Szczegóły
                  </span>
                  <div className="p-3 text-white">
                    <FaSearchPlus size={14} />
                  </div>
                </div>

                {order.status === "AWAITING_OPERATOR" && (
                  <button
                    onClick={() => fetchApplicants(order.id)}
                    className="px-6 h-10 bg-primary-300 text-primary-900 rounded-xl font-bold hover:bg-primary-400 transition-all active:scale-95 text-[10px] uppercase tracking-widest shadow-md"
                  >
                    Chętni
                  </button>
                )}

                {order.status === "OPEN" && (
                  <div
                    onClick={() => onEdit(order)}
                    className="group flex items-center bg-white/10 rounded-xl hover:bg-primary-500 hover:text-black transition-all cursor-pointer overflow-hidden h-10"
                  >
                    <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-black pl-0 group-hover:pl-4">
                      Edytuj
                    </span>
                    <div className="p-3 text-white group-hover:text-black transition-all">
                      <FaEdit size={14} />
                    </div>
                  </div>
                )}

                {order.status === "IN_PROGRESS" && (
                  <div
                    onClick={() => handleFinishOrder(order.id)}
                    className="group flex items-center bg-white/10 rounded-xl hover:bg-green-500 transition-all cursor-pointer overflow-hidden h-10"
                  >
                    <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-white pl-0 group-hover:pl-4">
                      Zakończ zlecenie
                    </span>
                    <div className="p-3 text-white">
                      <FaCheckCircle size={14} />
                    </div>
                  </div>
                )}

                {order.status !== "COMPLETED" &&
                  order.status !== "CANCELLED" && (
                    <div
                      onClick={() => handleCancelOrder(order.id)}
                      className="group flex items-center bg-white/10 rounded-xl hover:bg-red-600 transition-all cursor-pointer overflow-hidden h-10"
                    >
                      <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-white pl-0 group-hover:pl-4">
                        Anuluj
                      </span>
                      <div className="p-3 text-white">
                        <FaTrash size={14} />
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
          assignedOperatorId={(selectedOrder as any).operator_id}
        />
      )}

      {matchingOrderId && applicants[currentAppIndex] && (
        <OpMatch
          applicant={applicants[currentAppIndex]}
          onClose={() => setMatchingOrderId(null)}
          onAccept={(opId) => handleOpDecision(opId, "accept")}
          onReject={(opId) => handleOpDecision(opId, "reject")}
        />
      )}
    </div>
  );
}
