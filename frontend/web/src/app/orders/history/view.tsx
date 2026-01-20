"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { OrderResponse, OrderStatusLabels } from "../types";
import OrderDetailsModule from "../utils/details_module";
import ReviewModule from "../utils/review_module";
import { FaSearchPlus, FaStar, FaUserTie, FaTrophy, FaCheckCircle, FaTimesCircle } from "react-icons/fa";

interface OperatorInfo {
  name: string;
  surname: string;
}

type HistoryViewProps = Record<string, never>;

export default function HistoryView({}: HistoryViewProps) {
  const [myOrders, setMyOrders] = useState<OrderResponse[]>([]);
  const [operatorNames, setOperatorNames] = useState<
    Record<string, OperatorInfo>
  >({});
  const [reviewedOrderIds, setReviewedOrderIds] = useState<Set<string>>(
    new Set()
  );
  const [selectedOrder, setSelectedOrder] = useState<OrderResponse | null>(
    null
  );
  const [reviewingOrder, setReviewingOrder] = useState<OrderResponse | null>(
    null
  );
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem("reviewedOrderIds");
    if (stored) {
      try {
        const ids = JSON.parse(stored);
        setReviewedOrderIds(new Set(ids));
      } catch (err) {
        console.error("Error parsing reviewed orders:", err);
      }
    }
  }, []);

  const fetchOperatorInfo = useCallback(
    async (operatorId: string) => {
      if (operatorNames[operatorId]) {
        return operatorNames[operatorId];
      }

      try {
        const token = localStorage.getItem("token");
        const res = await fetch(`/operators/getOperatorProfile/${operatorId}`, {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          const operatorInfo = {
            name: data.name || "",
            surname: data.surname || "",
          };
          setOperatorNames((prev) => ({
            ...prev,
            [operatorId]: operatorInfo,
          }));
          return operatorInfo;
        }
      } catch (err) {
        console.error("Error fetching operator info:", err);
      }
      return { name: "Operator", surname: "" };
    },
    [operatorNames]
  );

  const fetchMyOrders = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/orders/getMyOrders`, {
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        const orders = Array.isArray(data) ? data : data.content || [];

        const filtered = orders.filter(
          (order: OrderResponse) =>
            order.status === "COMPLETED" || order.status === "CANCELLED"
        );
        setMyOrders(filtered);

        const operatorIds = filtered
          .filter(
            (order: OrderResponse) =>
              (order as unknown as Record<string, unknown>).operator_id
          )
          .map(
            (order: OrderResponse) =>
              (order as unknown as Record<string, unknown>).operator_id
          );

        for (const operatorId of operatorIds) {
          if (!operatorNames[operatorId as string]) {
            await fetchOperatorInfo(operatorId as string);
          }
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [operatorNames, fetchOperatorInfo]);

  useEffect(() => {
    fetchMyOrders();
  }, [fetchMyOrders]);

  const handleReviewSubmit = async (review: {
    rating: number;
    comment: string;
  }) => {
    if (!reviewingOrder) return;

    try {
      const token = localStorage.getItem("token");
      const operatorId = (reviewingOrder as unknown as Record<string, unknown>)
        .operator_id;

      if (
        !operatorId ||
        typeof operatorId !== "string" ||
        operatorId === "undefined"
      ) {
        throw new Error("Brak przypisanego operatora do tego zlecenia");
      }

      const res = await fetch(
        `/reviews/createReview/${reviewingOrder.id}/${operatorId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-USER-TOKEN": `Bearer ${token}`,
          },
          body: JSON.stringify({
            stars: review.rating,
            body: review.comment,
          }),
        }
      );

      if (res.ok) {
        const updatedSet = new Set([...reviewedOrderIds, reviewingOrder.id]);
        setReviewedOrderIds(updatedSet);
        localStorage.setItem(
          "reviewedOrderIds",
          JSON.stringify(Array.from(updatedSet))
        );
        setReviewingOrder(null);
      } else {
        const errorText = await res.text();
        console.error("Review submission error:", errorText);
        throw new Error(errorText || "Błąd przy wysyłaniu recenzji");
      }
    } catch (err) {
      console.error("Full error:", err);
      throw err;
    }
  };

  if (loading) {
    return (
      <div className="text-primary-800 font-bold py-20 text-center animate-pulse">
        Ładowanie historii...
      </div>
    );
  }

  if (myOrders.length === 0) {
    return (
      <div className="text-center py-20 text-gray-400 font-light animate-fadeIn">
        <p className="text-lg">Brak zleceń w historii</p>
        <p className="text-sm mt-2">
          Tutaj pojawią się Twoje zakończone i anulowane zlecenia
        </p>
      </div>
    );
  }

  // Statystyki dla klienta
  const stats = {
    completed: myOrders.filter((o) => o.status === "COMPLETED").length,
    cancelled: myOrders.filter((o) => o.status === "CANCELLED").length,
    reviewed: myOrders.filter((o) => reviewedOrderIds.has(o.id)).length,
    toReview: myOrders.filter((o) => o.status === "COMPLETED" && !reviewedOrderIds.has(o.id) && (o as unknown as Record<string, unknown>).operator_id).length,
  };

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-6 font-montserrat text-black">
      {/* Stats panel dla klienta */}
      <div className="grid grid-cols-2 gap-3">
        {/* Zlecenia jako klient */}
        <div className="bg-slate-900/90 backdrop-blur border border-white/5 rounded-2xl p-4">
          <div className="flex items-center gap-2 text-gray-400 text-xs uppercase tracking-wider font-bold mb-3">
            <FaUserTie size={14} />
            Jako klient
          </div>
          <div className="flex justify-around">
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-emerald-500/20 rounded-lg flex items-center justify-center">
                <FaCheckCircle className="text-emerald-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.completed}</p>
              <span className="text-[10px] text-gray-500 uppercase">Zrealizowanych</span>
            </div>
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-red-500/20 rounded-lg flex items-center justify-center">
                <FaTimesCircle className="text-red-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.cancelled}</p>
              <span className="text-[10px] text-gray-500 uppercase">Anulowanych</span>
            </div>
          </div>
        </div>

        {/* Recenzje do wystawienia */}
        <div className="bg-slate-900/90 backdrop-blur border border-white/5 rounded-2xl p-4">
          <div className="flex items-center gap-2 text-gray-400 text-xs uppercase tracking-wider font-bold mb-3">
            <FaStar size={14} />
            Recenzje
          </div>
          <div className="flex justify-around">
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-emerald-500/20 rounded-lg flex items-center justify-center">
                <FaCheckCircle className="text-emerald-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.reviewed}</p>
              <span className="text-[10px] text-gray-500 uppercase">Wystawione</span>
            </div>
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-amber-500/20 rounded-lg flex items-center justify-center">
                <FaStar className="text-amber-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.toReview}</p>
              <span className="text-[10px] text-gray-500 uppercase">Do oceny</span>
            </div>
          </div>
        </div>
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
                  <span
                    className={`${
                      order.status === "COMPLETED"
                        ? "text-green-400"
                        : "text-red-400"
                    }`}
                  >
                    {OrderStatusLabels[order.status]}
                  </span>
                </p>
              </div>

              <div className="flex flex-wrap justify-center items-center gap-3">
                {order.status === "COMPLETED" && (
                  <>
                    {(order as unknown as Record<string, unknown>)
                      .operator_id && (
                      <div
                        onClick={() =>
                          window.open(
                            `/user_profile?user_id=${
                              (order as unknown as Record<string, unknown>)
                                .operator_id
                            }`,
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

                    {!((order as unknown as Record<string, unknown>)
                      .reviewed as boolean) &&
                      !reviewedOrderIds.has(order.id) &&
                      ((order as unknown as Record<string, unknown>)
                        .operator_id as string) && (
                        <button
                          onClick={() => setReviewingOrder(order)}
                          className="group flex items-center bg-yellow-500/20 rounded-xl hover:bg-yellow-500/40 transition-all cursor-pointer overflow-hidden h-10"
                        >
                          <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-yellow-300 pl-0 group-hover:pl-4">
                            Oceń operatora
                          </span>
                          <div className="p-3 text-yellow-300">
                            <FaStar size={14} />
                          </div>
                        </button>
                      )}

                    {(((order as unknown as Record<string, unknown>)
                      .reviewed as boolean) ||
                      reviewedOrderIds.has(order.id)) && (
                      <div className="flex items-center gap-2 px-4 py-2 bg-green-500/20 rounded-xl text-green-300 text-[10px] font-bold uppercase tracking-widest">
                        <FaStar size={12} />
                        Oceniono
                      </div>
                    )}
                  </>
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
              </div>
            </div>
          </div>
        ))}
      </div>

      {selectedOrder && (
        <OrderDetailsModule
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          assignedOperatorId={
            (selectedOrder as unknown as Record<string, unknown>)
              .operator_id as string | undefined
          }
        />
      )}

      {reviewingOrder && (
        <ReviewModule
          operatorName={
            operatorNames[
              (reviewingOrder as unknown as Record<string, unknown>)
                .operator_id as string
            ]
              ? `${
                  operatorNames[
                    (reviewingOrder as unknown as Record<string, unknown>)
                      .operator_id as string
                  ].name
                } ${
                  operatorNames[
                    (reviewingOrder as unknown as Record<string, unknown>)
                      .operator_id as string
                  ].surname
                }`
              : "Operator"
          }
          onClose={() => setReviewingOrder(null)}
          onSubmit={handleReviewSubmit}
        />
      )}
    </div>
  );
}
