"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { OrderStatusLabels } from "../types";
import OrderDetailsModule from "../utils/details_module";
import ReviewModule from "../utils/review_module";
import { FaSearchPlus, FaStar, FaUser, FaCheckCircle, FaTimesCircle, FaTrophy, FaUserTie, FaHandshake } from "react-icons/fa";

interface MatchedOrderDto {
  id: string;
  client_id: string;
  operator_id?: string;
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
  isClientOrder?: boolean;
  status?: string;
}

interface ClientInfo {
  name: string;
  surname: string;
}

interface OperatorInfo {
  name: string;
  surname: string;
}

type OperatorHistoryViewProps = Record<string, never>;

export default function OperatorHistoryView({}: OperatorHistoryViewProps) {
  const [matchedOrders, setMatchedOrders] = useState<MatchedOrderDto[]>([]);
  const [clientNames, setClientNames] = useState<Record<string, ClientInfo>>(
    {}
  );
  const [operatorNames, setOperatorNames] = useState<
    Record<string, OperatorInfo>
  >({});
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
        const res = await fetch(`/user/getUserData?user_id=${clientId}`, {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
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

  const fetchMatchedOrders = useCallback(async () => {
    try {
      const token = localStorage.getItem("token");

      // Pobierz created ordery (gdzie był CLIENT)
      const myOrdersRes = await fetch(`/orders/getMyOrders?status=COMPLETED`, {
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      const myOrdersCanceled = await fetch(`/orders/getMyOrders?status=CANCELLED`, {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        }
      );

      // Pobierz matched ordery (gdzie był OPERATOR)
      const matchedOrdersRes = await fetch(
        `/operators/getMatchedOrders?size=100&status=COMPLETED&operator_status=ACCEPTED`,
        {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        }
      );

      const allOrders: MatchedOrderDto[] = [];

      // Created ordery - gdzie był CLIENT, ocenia OPERATORA
      if (myOrdersRes.ok) {
        const myOrdersData = await myOrdersRes.json();
        const myOrdersDataCanceled = await myOrdersCanceled.json();
        const myOrdersDataCombined = {
          content: [
            ...(myOrdersData.content || []),
            ...(myOrdersDataCanceled.content || []),
          ],
        };
        const myOrders = (myOrdersDataCombined.content || [])
          .filter(
            (order: MatchedOrderDto) =>
              order.status === "COMPLETED" || order.status === "CANCELLED"
          )
          .map(
            (order: MatchedOrderDto) =>
              ({
                ...order,
                order_status: order.status || "COMPLETED",
                isClientOrder: true, // Flag że był CLIENT
              } as MatchedOrderDto)
          );
        allOrders.push(...myOrders);
      }

      // Matched ordery - gdzie był OPERATOR, ocenia KLIENTA
      if (matchedOrdersRes.ok) {
        const matchedOrdersData = await matchedOrdersRes.json();
        const matchedOrders = (matchedOrdersData.content || [])
          .filter(
            (order: MatchedOrderDto) =>
              order.order_status === "COMPLETED" ||
              order.order_status === "CANCELLED"
          )
          .map((order: MatchedOrderDto) => ({
            ...order,
            isClientOrder: false, // Flag że był OPERATOR
          }));
        allOrders.push(...matchedOrders);
      }

      setMatchedOrders(allOrders);
      setLoading(false);

      // Pobierz nazwy klientów równolegle
      const clientIds = [...new Set(allOrders
        .filter((order: MatchedOrderDto) => !order.isClientOrder)
        .map((order: MatchedOrderDto) => order.client_id)
        .filter((id: string) => !clientNames[id]))];

      // Pobierz nazwy operatorów równolegle
      const operatorIds = [...new Set(allOrders
        .filter(
          (order: MatchedOrderDto) => order.isClientOrder && order.operator_id
        )
        .map((order: MatchedOrderDto) => order.operator_id as string)
        .filter((id: string) => !operatorNames[id]))];

      // Wykonaj wszystkie zapytania równolegle
      await Promise.all([
        ...clientIds.map((id) => fetchClientInfo(id)),
        ...operatorIds.map((id) => fetchOperatorInfo(id)),
      ]);
    } catch (err) {
      console.error(err);
      setLoading(false);
    }
  }, [clientNames, operatorNames, fetchClientInfo, fetchOperatorInfo]);

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
      const isClientOrder = reviewingOrder.isClientOrder;

      let targetId: string;
      let endpoint: string;

      if (isClientOrder) {
        // Byłeś CLIENT - oceniasz OPERATORA
        targetId = reviewingOrder.operator_id || "";
        endpoint = `/reviews/createReview/${reviewingOrder.id}/${targetId}`;

        if (!targetId || targetId === "undefined") {
          throw new Error("Brak przypisanego operatora do tego zlecenia");
        }
      } else {
        // Byłeś OPERATOR - oceniasz KLIENTA
        targetId = reviewingOrder.client_id;
        endpoint = `/reviews/createReview/${reviewingOrder.id}/${targetId}`;

        if (!targetId || targetId === "undefined") {
          throw new Error("Brak klienta w tym zleceniu");
        }
      }

      const res = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-USER-TOKEN": `Bearer ${token}`,
        },
        body: JSON.stringify({
          stars: review.rating,
          body: review.comment,
        }),
      });

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
      <div className="flex flex-col items-center justify-center py-20 text-white">
        <div className="w-12 h-12 border-4 border-primary-300 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-gray-400 text-sm uppercase tracking-widest">
          Ładowanie historii zleceń...
        </p>
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

  // Statystyki historii
  const stats = {
    completed: matchedOrders.filter((o) => o.order_status === "COMPLETED").length,
    cancelled: matchedOrders.filter((o) => o.order_status === "CANCELLED").length,
    asClient: matchedOrders.filter((o) => o.isClientOrder).length,
    asOperator: matchedOrders.filter((o) => !o.isClientOrder).length,
    reviewed: matchedOrders.filter((o) => reviewedOrderIds.has(o.id)).length,
    toReview: matchedOrders.filter((o) => o.order_status === "COMPLETED" && !reviewedOrderIds.has(o.id)).length,
  };

  return (
    <div className="w-full max-w-4xl animate-fadeIn space-y-6 font-montserrat text-black">
      {/* Stats panel */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        {/* Podsumowanie */}
        <div className="bg-slate-900/90 backdrop-blur border border-white/5 rounded-2xl p-4">
          <div className="flex items-center gap-2 text-gray-400 text-xs uppercase tracking-wider font-bold mb-3">
            <FaTrophy size={14} />
            Podsumowanie
          </div>
          <div className="flex justify-around">
            <div className="text-center">
              <p className="text-2xl font-bold text-emerald-400">{stats.completed}</p>
              <span className="text-[10px] text-gray-500 uppercase">Ukończonych</span>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold text-red-400">{stats.cancelled}</p>
              <span className="text-[10px] text-gray-500 uppercase">Anulowanych</span>
            </div>
          </div>
        </div>

        {/* Role */}
        <div className="bg-slate-900/90 backdrop-blur border border-white/5 rounded-2xl p-4">
          <div className="flex items-center gap-2 text-gray-400 text-xs uppercase tracking-wider font-bold mb-3">
            <FaHandshake size={14} />
            Twoja rola
          </div>
          <div className="flex justify-around">
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-blue-500/20 rounded-lg flex items-center justify-center">
                <FaUser className="text-blue-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.asClient}</p>
              <span className="text-[10px] text-gray-500 uppercase">Jako klient</span>
            </div>
            <div className="text-center">
              <div className="w-8 h-8 mx-auto mb-1 bg-amber-500/20 rounded-lg flex items-center justify-center">
                <FaUserTie className="text-amber-400" size={12} />
              </div>
              <p className="text-xl font-bold text-white">{stats.asOperator}</p>
              <span className="text-[10px] text-gray-500 uppercase">Jako operator</span>
            </div>
          </div>
        </div>

        {/* Recenzje */}
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
                  <span
                    className={`${
                      order.order_status === "COMPLETED"
                        ? "text-green-400"
                        : "text-red-400"
                    }`}
                  >
                    {OrderStatusLabels[order.order_status]}
                  </span>
                </p>
              </div>

              <div className="flex flex-wrap justify-center items-center gap-3">
                {order.order_status === "COMPLETED" && (
                  <>
                    {order.isClientOrder ? (
                      // Zlecenie gdzie był CLIENT - pokazuj OPERATORA
                      <>
                        <div
                          onClick={() =>
                            window.open(
                              `/user_profile?user_id=${order.operator_id}`,
                              "_blank"
                            )
                          }
                          className="group flex items-center bg-primary-300 rounded-xl hover:bg-primary-400 transition-all cursor-pointer overflow-hidden h-10 shadow-sm"
                        >
                          <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-primary-950 pl-0 group-hover:pl-4">
                            Wykonawca
                          </span>
                          <div className="p-3 text-primary-950">
                            <FaUser size={14} />
                          </div>
                        </div>

                        {!reviewedOrderIds.has(order.id) &&
                          order.operator_id && (
                            <button
                              onClick={() => setReviewingOrder(order)}
                              className="group flex items-center bg-yellow-500/20 rounded-xl hover:bg-yellow-500/40 transition-all cursor-pointer overflow-hidden h-10"
                            >
                              <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[200px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-yellow-300 pl-0 group-hover:pl-4">
                                Oceń wykonawcę
                              </span>
                              <div className="p-3 text-yellow-300">
                                <FaStar size={14} />
                              </div>
                            </button>
                          )}
                      </>
                    ) : (
                      // Zlecenie gdzie był OPERATOR - pokazuj KLIENTA
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
                      </>
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
            reviewingOrder.isClientOrder
              ? // Byłeś CLIENT - pokazuj nazwę OPERATORA
                reviewingOrder.operator_id &&
                operatorNames[reviewingOrder.operator_id]
                ? `${operatorNames[reviewingOrder.operator_id].name} ${
                    operatorNames[reviewingOrder.operator_id].surname
                  }`
                : "Operator"
              : // Byłeś OPERATOR - pokazuj nazwę KLIENTA
              clientNames[reviewingOrder.client_id]
              ? `${clientNames[reviewingOrder.client_id].name} ${
                  clientNames[reviewingOrder.client_id].surname
                }`
              : "Klient"
          }
          onClose={() => setReviewingOrder(null)}
          onSubmit={handleReviewSubmit}
          isClientReview={!reviewingOrder.isClientOrder}
        />
      )}
    </div>
  );
}
