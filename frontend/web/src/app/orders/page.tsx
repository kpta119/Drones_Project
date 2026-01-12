"use client";

import { useState } from "react";
import AvailableView from "./available/view";
import CreatedView from "./created/view"; // Sprawdź czy ścieżka jest OK
import ActiveView from "./active/view";
import CreateOrderView from "./create/view";

export type OrdersView = "available" | "created" | "active" | "create";

export default function OrdersPage() {
  const [view, setView] = useState<OrdersView>("available");

  return (
    <div className="min-h-screen bg-white font-montserrat">
      {/* MENU NAWIGACYJNE */}
      <div className="flex flex-wrap justify-center gap-4 lg:gap-8 py-10 px-4">
        <button
          onClick={() => setView("available")}
          className={`px-6 lg:px-12 py-3 lg:py-4 rounded-2xl font-semibold transition-all shadow-lg ${
            view === "available"
              ? "bg-primary-500 text-black"
              : "bg-gray-200 text-gray-500"
          }`}
        >
          Dostępne zlecenia
        </button>
        <button
          onClick={() => setView("created")}
          className={`px-6 lg:px-12 py-3 lg:py-4 rounded-2xl font-semibold transition-all shadow-lg ${
            view === "created" || view === "create"
              ? "bg-primary-500 text-black"
              : "bg-gray-200 text-gray-500"
          }`}
        >
          Własne zlecenia
        </button>
        <button
          onClick={() => setView("active")}
          className={`px-6 lg:px-12 py-3 lg:py-4 rounded-2xl font-semibold transition-all shadow-lg ${
            view === "active"
              ? "bg-primary-500 text-black"
              : "bg-gray-200 text-gray-500"
          }`}
        >
          Zlecenia w trakcie
        </button>
      </div>

      {/* GŁÓWNY CONTENT */}
      <main className="container mx-auto px-4 pb-20 flex justify-center">
        {view === "available" && <AvailableView />}

        {view === "created" && (
          <CreatedView onCreateNew={() => setView("create")} />
        )}

        {view === "active" && <ActiveView />}

        {view === "create" && (
          <CreateOrderView
            onCancel={() => setView("created")}
            onSuccess={() => setView("created")}
          />
        )}
      </main>
    </div>
  );
}
