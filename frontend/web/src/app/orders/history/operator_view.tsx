"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { OrderStatusLabels } from "../types";
import OrderDetailsModule from "../utils/details_module";
import ReviewModule from "../utils/review_module";
import { FaSearchPlus, FaStar, FaUser } from "react-icons/fa";
import { API_URL } from "../../config";

interface MatchedOrderDto {
  id: string;
  client_id: string;
  title: string;
  description: string;
  service: string;
  parameters: Record<string, string>;
  coordinates: string;
  distance: number;
  from_date: string;
  to_date: string;
  created_at: string;
  order_status:
    | "OPEN"
    | "AWAITING_OPERATOR"
    | "IN_PROGRESS"
    | "COMPLETED"
    | "CANCELLED";
  client_status: "PENDING" | "ACCEPTED" | "REJECTED";
  operator_status: "PENDING" | "ACCEPTED" | "REJECTED";
}

interface ClientInfo {
  name: string;
  surname: string;
}

type OperatorHistoryViewProps = Record<string, never>;

export default function OperatorHistoryView({}: OperatorHistoryViewProps) {
  const [matchedOrders, setMatchedOrders] = useState<MatchedOrderDto[]>([]);
  const [clientNames, setClientNames] = useState<Record<string, ClientInfo>>(
    {}
  );
  const [reviewedOrderIds, setReviewedOrderIds] = useState<Set<string>>(
    new Set()
  );
  const [selectedOrder, setSelectedOrder] = useState<MatchedOrderDto | null>(
    null
  );
  const [reviewingOrder, setReviewingOrder] = useState<MatchedOrderDto | null>(
    null
  );
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem("reviewedClientOrderIds");
    if (stored) {
      try {
        const ids = JSON.parse(stored);
        setReviewedOrderIds(new Set(ids));
      } catch (err) {
        console.error("Error parsing reviewed orders:", err);
      }
    }
  }, []);

  const fetchClientInfo = useCallback(
    async (clientId: string) => {
      if (clientNames[clientId]) {
        return clientNames[clientId];
      }

      try {
        const token = localStorage.getItem("token");
        const res = await fetch(
          `${API_URL}/api/user/getUserData?user_id=${clientId}`,
          {
            headers: { "X-USER-TOKEN": `Bearer ${token}` },
          }
        );
        if (res.ok) {
          const data = await res.json();
          const clientInfo = {
            name: data.name || "",
            surname: data.surname || "",
          };
          setClientNames((prev) => ({
            ...prev,
            [clientId]: clientInfo,
          }));
          return clientInfo;
        }
      } catch (err) {
        console.error("Error fetching client info:", err);
      }
      return { name: "Klient", surname: "" };
    },
    [clientNames]
  );

  const fetchMatchedOrders = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(
        `${API_URL}/api/operators/getMatchedOrders?size=100`,
        {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        }
      );
      if (res.ok) {
        const data = await res.json();
        const filtered = (data.content || []).filter(
          (order: MatchedOrderDto) =>
            order.order_status === "COMPLETED" &&
            order.operator_status === "ACCEPTED" &&
            order.client_status === "ACCEPTED"
        );
        setMatchedOrders(filtered);

        const clientIds = filtered
          .map((order: MatchedOrderDto) => order.client_id)
          .filter((id: string) => !clientNames[id]);

        for (const clientId of clientIds) {
          await fetchClientInfo(clientId);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [clientNames, fetchClientInfo]);

  useEffect(() => {
    fetchMatchedOrders();
  }, [fetchMatchedOrders]);

  const handleReviewSubmit = async (review: {
    rating: number;
    comment: string;
  }) => {
    if (!reviewingOrder) return;

    try {
      const token = localStorage.getItem("token");
      const clientId = reviewingOrder.client_id;

      console.log("Submitting review for client:", {
        orderId: reviewingOrder.id,
        clientId,
        rating: review.rating,
        comment: review.comment,
      });

      if (
        !clientId ||
        typeof clientId !== "string" ||
        clientId === "undefined"
      ) {
        throw new Error("Brak przypisanego klienta do tego zlecenia");
      }

      const res = await fetch(
        `${API_URL}/api/reviews/createReview/${reviewingOrder.id}/${clientId}`,
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
          "reviewedClientOrderIds",
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
        Ładowanie historii zleceń...
      </div>
    );
  }

  if (matchedOrders.length === 0) {
    return (
      <div className="text-center py-20 text-gray-400 font-light animate-fadeIn">
        <p className="text-lg">Brak zakończonych zleceń</p>
        <p className="text-sm mt-2">
          Tutaj pojawią się Twoje zakończone zlecenia, które możesz ocenić
        </p>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-6 font-montserrat text-black">
      <div className="space-y-6">
        {matchedOrders.map((order) => (
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
                  <span className="text-green-400">
                    {OrderStatusLabels[order.order_status]}
                  </span>
                </p>
              </div>

              <div className="flex flex-wrap justify-center items-center gap-3">
                {order.order_status === "COMPLETED" && (
                  <>
                    <div
                      onClick={() =>
                        window.open(
                          `/user_profile?user_id=${order.client_id}`,
                          "_blank"
                        )
                      }
                      className="group flex items-center bg-primary-300 rounded-xl hover:bg-primary-400 transition-all cursor-pointer overflow-hidden h-10 shadow-sm"
                    >
                      <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-primary-950 pl-0 group-hover:pl-4">
                        Klient
                      </span>
                      <div className="p-3 text-primary-950">
                        <FaUser size={14} />
                      </div>
                    </div>

                    {!reviewedOrderIds.has(order.id) && (
                      <button
                        onClick={() => setReviewingOrder(order)}
                        className="group flex items-center bg-yellow-500/20 rounded-xl hover:bg-yellow-500/40 transition-all cursor-pointer overflow-hidden h-10"
                      >
                        <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-yellow-300 pl-0 group-hover:pl-4">
                          Oceń klienta
                        </span>
                        <div className="p-3 text-yellow-300">
                          <FaStar size={14} />
                        </div>
                      </button>
                    )}

                    {reviewedOrderIds.has(order.id) && (
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
          order={{
            id: selectedOrder.id,
            client_id: selectedOrder.client_id,
            title: selectedOrder.title,
            description: selectedOrder.description,
            service: selectedOrder.service,
            coordinates: selectedOrder.coordinates,
            from_date: selectedOrder.from_date,
            to_date: selectedOrder.to_date,
            status: selectedOrder.order_status,
            created_at: selectedOrder.created_at,
            parameters: selectedOrder.parameters,
          }}
          onClose={() => setSelectedOrder(null)}
          assignedOperatorId={undefined}
        />
      )}

      {reviewingOrder && (
        <ReviewModule
          operatorName={
            clientNames[reviewingOrder.client_id]
              ? `${clientNames[reviewingOrder.client_id].name} ${
                  clientNames[reviewingOrder.client_id].surname
                }`
              : "Klient"
          }
          onClose={() => setReviewingOrder(null)}
          onSubmit={handleReviewSubmit}
          isClientReview={true}
        />
      )}
    </div>
  );
}
