"use client";

import { ActiveOrderDto, OrderStatusLabels } from "../types";
import AddToCalButton from "./add_to_cal_button";

export default function ActiveView() {
  const mockActive: ActiveOrderDto[] = [
    {
      id: "active-1",
      city: "Kraków",
      street: "Rynek Główny 1",
      title: "Inspekcja dachu kamienicy",
      status: "IN_PROGRESS",
      service: "Inspekcja techniczna",
      description: "Wymagane zdjęcia wysokiej rozdzielczości kominów.",
      createdAt: "2024-01-05",
      coordinates: "50.0614,19.9365",
      fromDate: "2024-01-15",
      toDate: "2024-01-16",
      startDate: "2024-01-15",
      endDate: "2024-01-16",
      client_id: "user-1",
      parameters: {},
    },
  ];

  return (
    <div className="w-full max-w-4xl space-y-4 animate-fadeIn">
      {mockActive.map((order) => (
        <div
          key={order.id}
          className="bg-white border-2 border-gray-100 rounded-3xl p-6 shadow-sm flex flex-col md:flex-row justify-between items-center gap-4 hover:border-primary-200 transition-colors"
        >
          <div className="flex gap-4 items-center">
            <div className="w-16 h-16 bg-primary-100 rounded-2xl flex items-center justify-center text-2xl shadow-inner">
              🛸
            </div>
            <div>
              <h3 className="font-bold text-lg text-slate-800">
                {order.title}
              </h3>
              <p className="text-gray-500 text-sm">
                {order.city}, {order.street}
              </p>
              <p className="text-xs text-primary-600 font-bold mt-1 uppercase tracking-wider">
                {OrderStatusLabels[order.status]}
              </p>
            </div>
          </div>

          <div className="flex flex-col items-center md:items-end gap-2">
            <p className="text-sm font-medium text-gray-700">
              Termin: {order.startDate}
            </p>
            <AddToCalButton orderId={order.id} />
          </div>
        </div>
      ))}
    </div>
  );
}
