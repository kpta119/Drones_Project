"use client";

import { ActiveOrderDto, OrderStatusLabels } from "../types";
import AddToCalButton from "./add_to_cal_button";
import { FaLock, FaUserPlus, FaRobot } from "react-icons/fa";

export default function ActiveView({ isOperator }: { isOperator: boolean }) {
  const mockActive: ActiveOrderDto[] = [
    {
      id: "active-1",
      title: "Przykładowe zlecenie",
      status: "IN_PROGRESS",
      service: "Skaning",
      description: "Opis",
      createdAt: "2024",
      coordinates: "0,0",
      from_date: "2024",
      to_date: "2024",
      startDate: "2024-01-20",
      endDate: "2024",
      client_id: "1",
      parameters: {},
      city: "Poznań",
      street: "ul. Nowa 12",
    },
  ];

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
          swoimi aktywnymi zleceniami.
        </p>
        <button
          onClick={() => (window.location.href = "/user_profile")}
          className="mt-10 px-12 py-4 bg-primary-300 text-black rounded-2xl font-black hover:bg-primary-400 transition-all shadow-xl flex items-center gap-3 border-2 border-primary-500/20 uppercase tracking-widest text-sm"
        >
          <FaUserPlus />
          Załóż profil operatora
        </button>
      </div>
    );
  }

  return (
    <div className="w-full max-w-4xl space-y-4 animate-fadeIn font-montserrat">
      {mockActive.map((order) => (
        <div
          key={order.id}
          className="bg-white border-2 border-primary-100 rounded-2rem p-6 shadow-sm flex flex-col md:flex-row justify-between items-center gap-4 hover:border-primary-300 transition-all group"
        >
          <div className="flex gap-5 items-center">
            <div className="w-16 h-16 bg-primary-100 rounded-2xl flex items-center justify-center text-3xl shadow-sm text-primary-800 group-hover:scale-110 transition-transform">
              <FaRobot />
            </div>
            <div>
              <h3 className="font-bold text-lg text-primary-950">
                {order.title}
              </h3>
              <p className="text-gray-500 text-sm font-medium">
                {order.city}, {order.street}
              </p>
              <p className="text-[10px] text-primary-700 font-black mt-1 uppercase tracking-widest bg-primary-50 px-2 py-0.5 rounded-md border border-primary-100 inline-block">
                {OrderStatusLabels[order.status]}
              </p>
            </div>
          </div>
          <div className="flex flex-col items-center md:items-end gap-3 text-black">
            <p className="text-sm font-black text-gray-700 uppercase tracking-tighter">
              Termin: {order.startDate}
            </p>
            <AddToCalButton orderId={order.id} />
          </div>
        </div>
      ))}
    </div>
  );
}
