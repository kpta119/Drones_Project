"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { OrderResponse, OrderStatusLabels } from "../types";
import OrderDetailsModule from "../utils/details_module";
import ReviewModule from "../utils/review_module";
import { FaSearchPlus, FaStar, FaUserTie } from "react-icons/fa";

interface OperatorInfo {
  name: string;
  surname: string;
}

interface HistoryViewProps {
  onEdit?: (order: OrderResponse) => void;
}

export default function HistoryView({ onEdit }: HistoryViewProps) {
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
        const res = await fetch(
          `/api/operators/getOperatorProfile/${operatorId}`,
          {
            headers: { "X-USER-TOKEN": `Bearer ${token}` },
          }
        );
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
      const res = await fetch("/api/orders/getMyOrders", {
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
          .filter((order: any) => order.operator_id)
          .map((order: any) => order.operator_id);

        for (const operatorId of operatorIds) {
          if (!operatorNames[operatorId]) {
            await fetchOperatorInfo(operatorId);
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
      const operatorId = (reviewingOrder as any).operator_id;

      console.log("Submitting review:", {
        orderId: reviewingOrder.id,
        operatorId,
        rating: review.rating,
        comment: review.comment,
      });

      if (
        !operatorId ||
        typeof operatorId !== "string" ||
        operatorId === "undefined"
      ) {
        throw new Error("Brak przypisanego operatora do tego zlecenia");
      }

      const res = await fetch(
        `/api/reviews/createReview/${reviewingOrder.id}/${operatorId}`,
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

      console.log("Review response status:", res.status);

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

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-6 font-montserrat text-black">
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
                    {(order as any).operator_id && (
                      <div
                        onClick={() =>
                          window.open(
                            `/user_profile?user_id=${
                              (order as any).operator_id
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

                    {!(order as any).reviewed &&
                      !reviewedOrderIds.has(order.id) &&
                      (order as any).operator_id && (
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

                    {((order as any).reviewed ||
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
          assignedOperatorId={(selectedOrder as any).operator_id}
        />
      )}

      {reviewingOrder && (
        <ReviewModule
          operatorId={(reviewingOrder as any).operator_id}
          operatorName={
            operatorNames[(reviewingOrder as any).operator_id]
              ? `${operatorNames[(reviewingOrder as any).operator_id].name} ${
                  operatorNames[(reviewingOrder as any).operator_id].surname
                }`
              : "Operator"
          }
          orderId={reviewingOrder.id}
          onClose={() => setReviewingOrder(null)}
          onSubmit={handleReviewSubmit}
        />
      )}
    </div>
  );
}
