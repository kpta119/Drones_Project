"use client";

import { useEffect, useState } from "react";
import { OrderResponse, OrderStatusLabels } from "../types";
import AddToCalButton from "./add_to_cal_button";
import {
  FaLock,
  FaUserPlus,
  FaClipboardList,
  FaSearchPlus,
  FaUserAlt,
} from "react-icons/fa";
import { getAddressFromCoordinates } from "../utils/geocoding";
import OrderDetailsModule from "../utils/details_module";
import { API_URL } from "../../config";

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

  useEffect(() => {
    if (!isOperator) {
      setLoading(false);
      return;
    }

    const fetchActiveOrders = async () => {
      try {
        const token = localStorage.getItem("token");
        const res = await fetch(
          `${API_URL}/api/calendar/getInProgressSchedulableOrders?size=100`,
          {
            headers: { "X-USER-TOKEN": `Bearer ${token}` },
          }
        );

        if (res.ok) {
          const data = await res.json();
          const ordersArray = data.content || [];
          const enrichedOrders = await Promise.all(
            ordersArray.map(async (order: Record<string, unknown>) => {
              const addr = await getAddressFromCoordinates(
                order.coordinates as string
              );
              const alreadyAdded =
                (order.is_already_added as boolean | null) ?? null;
              return {
                ...order,
                alreadyAdded,
                city: addr.city,
                street: addr.street,
              };
            })
          );
          setActiveOrders(enrichedOrders);
        }
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchActiveOrders();
  }, [isOperator]);

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
      <div className="text-primary-800 font-bold py-20 text-center animate-pulse">
        Ładowanie Twoich prac...
      </div>
    );

  return (
    <div className="w-full max-w-5xl space-y-4 animate-fadeIn font-montserrat">
      {activeOrders.length > 0 ? (
        activeOrders.map((order) => (
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
                  {order.city}, {order.street}
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
              />
            </div>
          </div>
        ))
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
