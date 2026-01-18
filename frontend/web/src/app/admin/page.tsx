"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { API_URL } from "../config";

interface SystemStats {
  users: {
    clients: number;
    operators: number;
  };
  orders: {
    active: number;
    completed: number;
    avgPerOperator: number;
  };
  operators: {
    busy: number;
    topOperator: {
      operatorId: string;
      completedOrders: number;
    } | null;
  };
  reviews: {
    total: number;
  };
}

export default function AdminDashboard() {
  const router = useRouter();
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const checkAuth = () => {
      const token = localStorage.getItem("token");
      const role = localStorage.getItem("role");

      // Redirect to login if no token
      if (!token) {
        router.replace("/login");
        return;
      }

      // Redirect to orders if not ADMIN
      if (role !== "ADMIN") {
        router.replace("/orders");
        return;
      }

      fetchStats();
    };

    checkAuth();
  }, [router]);

  const fetchStats = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem("token");

      const url = `${API_URL}/api/admin/getStats`;

      const response = await fetch(url, {
        method: "GET",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error(
          `Failed to fetch stats: ${response.status} ${response.statusText}`
        );
      }

      const data = (await response.json()) as SystemStats;
      setStats(data);
      setError("");
    } catch (err: unknown) {
      const error =
        err instanceof Error ? err.message : "Failed to load statistics";
      setError(error);
    } finally {
      setLoading(false);
    }
  };

  if (loading)
    return <div className="text-gray-600">Ładowanie statystyk...</div>;
  if (error) return <div className="text-red-600">Błąd: {error}</div>;
  if (!stats) return <div className="text-gray-600">Brak danych</div>;

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Panel Administracyjny</h1>

      <div className="grid grid-cols-2 gap-5 mt-5">
        <div className="border border-gray-300 p-5 rounded">
          <h2 className="text-xl font-semibold mb-3">Użytkownicy</h2>
          <p className="mb-2">
            Klienci: <strong>{stats.users.clients}</strong>
          </p>
          <p className="mb-2">
            Operatorzy: <strong>{stats.users.operators}</strong>
          </p>
          <p>
            Razem:{" "}
            <strong>{stats.users.clients + stats.users.operators}</strong>
          </p>
        </div>

        <div className="border border-gray-300 p-5 rounded">
          <h2 className="text-xl font-semibold mb-3">Zlecenia</h2>
          <p className="mb-2">
            Aktywne: <strong>{stats.orders.active}</strong>
          </p>
          <p className="mb-2">
            Ukończone: <strong>{stats.orders.completed}</strong>
          </p>
          <p>
            Średnio na operatora:{" "}
            <strong>{stats.orders.avgPerOperator.toFixed(2)}</strong>
          </p>
        </div>

        <div className="border border-gray-300 p-5 rounded">
          <h2 className="text-xl font-semibold mb-3">Operatorzy</h2>
          <p className="mb-2">
            Zajęci: <strong>{stats.operators.busy}</strong>
          </p>
          {stats.operators.topOperator && (
            <>
              <p className="mb-2">
                Top operator:{" "}
                <strong>{stats.operators.topOperator.operatorId}</strong>
              </p>
              <p>
                Ukończone zlecenia:{" "}
                <strong>{stats.operators.topOperator.completedOrders}</strong>
              </p>
            </>
          )}
        </div>

        <div className="border border-gray-300 p-5 rounded">
          <h2 className="text-xl font-semibold mb-3">Opinie</h2>
          <p>
            Razem opinii: <strong>{stats.reviews.total}</strong>
          </p>
        </div>
      </div>

      <div className="mt-10">
        <button
          onClick={fetchStats}
          className="px-5 py-2 bg-blue-600 text-white rounded cursor-pointer hover:bg-blue-700"
        >
          Odśwież
        </button>
      </div>
    </div>
  );
}
