// src/app/orders/created/view.tsx
"use client";

import { useEffect, useState } from "react";
import { OrderResponse } from "../types";

export default function CreatedView() {
  const [myOrders, setMyOrders] = useState<OrderResponse[]>([]);

  useEffect(() => {
    const fetchMyOrders = async () => {
      const token = localStorage.getItem("token");
      const res = await fetch("/api/orders/getMyOrders", {
        headers: { "X-USER-TOKEN": `Bearer ${token}` },
      });
      if (res.ok) setMyOrders(await res.json());
    };
    fetchMyOrders();
  }, []);

  return (
    <div className="w-full max-w-4xl space-y-6">
      {myOrders.map((order) => (
        <div
          key={order.id}
          className="bg-slate-900 rounded-[2.5rem] p-8 text-white relative overflow-hidden"
        >
          <div className="flex justify-between items-center relative z-10">
            <div>
              <h3 className="text-xl font-bold">{order.title}</h3>
              <p className="text-gray-400">{order.status}</p>
            </div>

            {/* Przycisk pojawia się tylko gdy faza to Matching */}
            {order.status === "AWAITING_OPERATOR" && (
              <button className="bg-primary-500 text-black px-6 py-2 rounded-xl font-bold">
                Przejrzyj chętnych
              </button>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
