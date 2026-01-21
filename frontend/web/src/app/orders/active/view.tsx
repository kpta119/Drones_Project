"use client";

import { useEffect, useState, useCallback } from "react";
import { OrderResponse, OrderStatusLabels } from "../types";
import AddToCalButton from "./add_to_cal_button";
import {
  FaLock,
  FaUserPlus,
  FaClipboardList,
  FaSearchPlus,
  FaUserAlt,
  FaCalendarAlt,
  FaMapMarkerAlt,
  FaRocket,
  FaCheckCircle,
} from "react-icons/fa";
import { getAddressFromCoordinates } from "../utils/geocoding";
import OrderDetailsModule from "../utils/details_module";

interface SchedulableOrder extends OrderResponse {
  alreadyAdded?: boolean | null;
  city?: string;
  street?: string;
}

export default function ActiveView({ isOperator }: { isOperator: boolean }) {
  const [activeOrders, setActiveOrders] = useState<SchedulableOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState<SchedulableOrder | null>(
    null
  );

  const handleCalendarAdded = (orderId: string) => {
    setActiveOrders((prev) =>
      prev.map((order) =>
        order.id === orderId ? { ...order, alreadyAdded: true } : order
      )
    );
  };

  const fetchAddressForOrder = useCallback(async (orderId: string, coordinates: string) => {
    try {
      const addr = await getAddressFromCoordinates(coordinates);
      setActiveOrders((prev) =>
        prev.map((order) =>
          order.id === orderId
            ? { ...order, city: addr.city, street: addr.street }
            : order
        )
      );
    } catch (err) {
      console.error("Error fetching address:", err);
    }
  }, []);

  useEffect(() => {
    if (!isOperator) {
      setLoading(false);
      return;
    }

    const fetchActiveOrders = async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch(
          `/calendar/getInProgressSchedulableOrders?size=100`,
          {
            headers: { "X-USER-TOKEN": `Bearer ${token}` },
          }
        );

        if (res.ok) {
          const data = await res.json();
          const ordersArray = data.content || [];
          const orders: SchedulableOrder[] = ordersArray.map(
            (order: Record<string, unknown>) => ({
              ...order,
              alreadyAdded: (order.is_already_added as boolean | null) ?? null,
              city: "",
              street: "",
            })
          );
          setActiveOrders(orders);
          setLoading(false);

          // Pobierz adresy asynchronicznie po załadowaniu strony
          orders.forEach((order) => {
            fetchAddressForOrder(order.id, order.coordinates);
          });
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchActiveOrders();
  }, [isOperator, fetchAddressForOrder]);

  if (!isOperator) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center animate-fadeIn text-black font-montserrat">
        <div className="w-24 h-24 bg-primary-100 rounded-full flex items-center justify-center text-4xl mb-8 shadow-inner border border-primary-200">
          <FaLock className="text-primary-700" />
        </div>
        <h2 className="text-3xl font-bold mb-4 text-primary-900 uppercase tracking-tight">
          Twój panel pracy
        </h2>
        <p className="text-gray-500 max-w-md font-medium leading-relaxed px-4">
          Musisz posiadać zweryfikowany profil operatora, aby móc zarządzać
          swoimi aktywnymi zleceniami jako operator.
        </p>
        <button
          onClick={() => (window.location.href = "/user_profile")}
          className="mt-10 px-12 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold hover:bg-primary-400 transition-all shadow-xl flex items-center gap-3 border-2 border-primary-500/20 uppercase tracking-widest text-sm"
        >
          <FaUserPlus />
          Załóż profil operatora
        </button>
      </div>
    );
  }

  if (loading)
    return (
      <div className="flex flex-col items-center justify-center py-20 text-white">
        <div className="w-12 h-12 border-4 border-primary-300 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-gray-400 text-sm uppercase tracking-widest">
          Ładowanie Twoich prac...
        </p>
      </div>
    );

  // Znajdź najbliższe zlecenie po dacie
  const sortedByDate = [...activeOrders].sort(
    (a, b) => new Date(a.from_date).getTime() - new Date(b.from_date).getTime()
  );
  const nextOrder = sortedByDate[0];
  const addedToCalendar = activeOrders.filter((o) => o.alreadyAdded).length;

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("pl-PL", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  };

  return (
    <div className="w-full max-w-5xl space-y-6 animate-fadeIn font-montserrat">
      {activeOrders.length > 0 ? (
        <>
          {/* Info panel */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Następne zlecenie */}
            {nextOrder && (
              <div className="md:col-span-2 bg-gradient-to-br from-primary-600 to-primary-800 rounded-2xl p-5 text-white relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl -translate-y-1/2 translate-x-1/2" />
                <div className="relative z-10">
                  <div className="flex items-center gap-2 text-primary-200 text-xs uppercase tracking-widest font-bold mb-2">
                    <FaRocket size={12} />
                    Najbliższy deadline
                  </div>
                  <h3 className="text-xl font-bold mb-3 truncate">{nextOrder.title}</h3>
                  <div className="flex flex-wrap gap-4 text-lg text-primary-100">
                    <span className="flex items-center gap-2 font-bold">
                      <FaCalendarAlt size={16} />
                      {formatDate(nextOrder.from_date)}
                    </span>
                    {nextOrder.city && (
                      <span className="flex items-center gap-2 font-bold">
                        <FaMapMarkerAlt size={16} />
                        {nextOrder.city}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Mini statystyki */}
            <div className="flex flex-col gap-4">
              <div className="bg-white border-2 border-primary-100 rounded-2xl p-4 flex items-center gap-4">
                <div className="w-12 h-12 bg-primary-100 rounded-xl flex items-center justify-center">
                  <FaClipboardList className="text-primary-600 text-xl" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-primary-900">{activeOrders.length}</p>
                  <span className="text-xs text-gray-500 uppercase tracking-wider font-semibold">Aktywnych zleceń</span>
                </div>
              </div>
              <div className="bg-white border-2 border-primary-100 rounded-2xl p-4 flex items-center gap-4">
                <div className="w-12 h-12 bg-emerald-100 rounded-xl flex items-center justify-center">
                  <FaCheckCircle className="text-emerald-600 text-xl" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-primary-900">{addedToCalendar}</p>
                  <span className="text-xs text-gray-500 uppercase tracking-wider font-semibold">W kalendarzu</span>
                </div>
              </div>
            </div>
          </div>

          {/* Lista zleceń */}
          <div className="space-y-4">
            {activeOrders.map((order) => (
          <div
            key={order.id}
            className="bg-white border-2 border-primary-100 rounded-[2.5rem] p-6 shadow-sm flex flex-col md:flex-row justify-between items-center gap-4 hover:border-primary-300 transition-all"
          >
            <div className="flex gap-5 items-center flex-1 text-black">
              <div className="w-16 h-16 bg-primary-100 rounded-2xl flex items-center justify-center text-3xl text-primary-800 shrink-0">
                <FaClipboardList />
              </div>
              <div>
                <h3 className="font-bold text-lg text-primary-950 leading-tight">
                  {order.title}
                </h3>
                <p className="text-gray-500 text-sm font-medium">
                  {order.city && order.street
                    ? `${order.city}, ${order.street}`
                    : <span className="text-gray-400 animate-pulse">Ładowanie adresu...</span>}
                </p>
                <div className="flex gap-2 mt-1">
                  <span className="text-[10px] text-primary-700 font-bold uppercase tracking-widest bg-primary-50 px-2 py-0.5 rounded-md border border-primary-100">
                    {OrderStatusLabels[order.status]}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-row items-center gap-3 shrink-0">
              <div
                onClick={() =>
                  window.open(
                    `/user_profile?user_id=${order.client_id}`,
                    "_blank"
                  )
                }
                className="group flex items-center bg-primary-50 rounded-xl hover:bg-primary-200 transition-all cursor-pointer overflow-hidden h-12"
              >
                <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-primary-900 pl-0 group-hover:pl-4">
                  Profil autora
                </span>
                <div className="p-4 text-primary-700 group-hover:text-primary-900">
                  <FaUserAlt size={14} />
                </div>
              </div>

              <div
                onClick={() => setSelectedOrder(order)}
                className="group flex items-center bg-primary-50 rounded-xl hover:bg-primary-200 transition-all cursor-pointer overflow-hidden h-12"
              >
                <span className="max-w-0 overflow-hidden whitespace-nowrap opacity-0 group-hover:max-w-[150px] group-hover:opacity-100 transition-all duration-500 ease-in-out font-bold text-[10px] uppercase tracking-widest text-primary-900 pl-0 group-hover:pl-4">
                  Szczegóły
                </span>
                <div className="p-4 text-primary-700 group-hover:text-primary-900">
                  <FaSearchPlus size={14} />
                </div>
              </div>

              <AddToCalButton
                orderId={order.id}
                alreadyAdded={order.alreadyAdded}
                onAdded={() => handleCalendarAdded(order.id)}
              />
            </div>
          </div>
        ))
          }
          </div>
        </>
      ) : (
        <div className="text-center py-20 text-gray-400 font-bold uppercase tracking-widest">
          Nie masz obecnie żadnych aktywnych zleceń.
        </div>
      )}
      {selectedOrder && (
        <OrderDetailsModule
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
        />
      )}
    </div>
  );
}
