"use client";

import { useState, useEffect } from "react";
import AvailableView from "./available/view";
import CreatedView from "./created/view";
import ActiveView from "./active/view";
import HistoryView from "./history/view";
import OperatorHistoryView from "./history/operator_view";
import CreateOrderView from "./create/view";
import { OrderResponse } from "./types";

export type OrdersView =
  | "available"
  | "created"
  | "active"
  | "history"
  | "create";

export default function OrdersPage() {
  const [view, setView] = useState<OrdersView>("created");
  const [userRole, setUserRole] = useState<string | null>(null);
  const [editingOrder, setEditingOrder] = useState<OrderResponse | null>(null);

  useEffect(() => {
    const role = localStorage.getItem("role");
    setUserRole(role ? role.toUpperCase() : "CLIENT");
  }, []);

  const handleEdit = (order: OrderResponse) => {
    setEditingOrder(order);
    setView("create");
  };

  const handleCreateNew = () => {
    setEditingOrder(null);
    setView("create");
  };

  const handleBackToCreated = () => {
    setEditingOrder(null);
    setView("created");
  };

  return (
    <div className="min-h-screen bg-white font-montserrat text-black">
      <div className="flex flex-col lg:flex-row items-center justify-center gap-4 lg:gap-24 py-10 px-6">
        <button
          onClick={() => setView("available")}
          className={`w-full max-w-[300px] lg:w-auto lg:px-12 py-2.5 rounded-xl font-bold transition-all shadow-md border-2 ${
            view === "available"
              ? "bg-primary-300 border-primary-400 text-primary-900"
              : "bg-gray-100 border-transparent text-gray-500 hover:bg-gray-200"
          }`}
        >
          Dostępne zlecenia
        </button>
        <button
          onClick={() => setView("created")}
          className={`w-full max-w-[300px] lg:w-auto lg:px-12 py-2.5 rounded-xl font-bold transition-all shadow-md border-2 ${
            view === "created" || view === "create"
              ? "bg-primary-300 border-primary-400 text-primary-900"
              : "bg-gray-100 border-transparent text-gray-500 hover:bg-gray-200"
          }`}
        >
          Własne zlecenia
        </button>
        <button
          onClick={() => setView("active")}
          className={`w-full max-w-[300px] lg:w-auto lg:px-12 py-2.5 rounded-xl font-bold transition-all shadow-md border-2 ${
            view === "active"
              ? "bg-primary-300 border-primary-400 text-primary-900"
              : "bg-gray-100 border-transparent text-gray-500 hover:bg-gray-200"
          }`}
        >
          Zlecenia w trakcie
        </button>
        <button
          onClick={() => setView("history")}
          className={`w-full max-w-[300px] lg:w-auto lg:px-12 py-2.5 rounded-xl font-bold transition-all shadow-md border-2 ${
            view === "history"
              ? "bg-primary-300 border-primary-400 text-primary-900"
              : "bg-gray-100 border-transparent text-gray-500 hover:bg-gray-200"
          }`}
        >
          Historia zleceń
        </button>
      </div>

      <main className="container mx-auto px-4 pb-20 flex justify-center">
        {view === "available" && (
          <AvailableView isOperator={userRole === "OPERATOR"} />
        )}
        {view === "created" && (
          <CreatedView onCreateNew={handleCreateNew} onEdit={handleEdit} />
        )}
        {view === "active" && (
          <ActiveView isOperator={userRole === "OPERATOR"} />
        )}
        {view === "history" && userRole === "OPERATOR" && (
          <OperatorHistoryView onEdit={handleEdit} />
        )}
        {view === "history" && userRole !== "OPERATOR" && (
          <HistoryView onEdit={handleEdit} />
        )}
        {view === "create" && (
          <CreateOrderView
            onCancel={handleBackToCreated}
            onSuccess={handleBackToCreated}
            editData={editingOrder}
          />
        )}
      </main>
    </div>
  );
}
