"use client";

import { useEffect, useState, useCallback } from "react";

interface Order {
  order_id: string;
  client_id: string;
  operator_id: string;
  title: string;
  coordinates: string;
  status: string;
  service_name: string;
  created_at: string;
}

interface ApiResponse<T> {
  content: T[];
  page: {
    totalPages: number;
  };
}

const getStatusColor = (status: string): string => {
  const statusColors: Record<string, string> = {
    OPEN: "bg-yellow-100",
    AWAITING_OPERATOR: "bg-blue-100",
    IN_PROGRESS: "bg-purple-100",
    COMPLETED: "bg-green-100",
    CANCELLED: "bg-red-100",
  };
  return statusColors[status] || "bg-gray-100";
};

const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    OPEN: "Otwarte",
    AWAITING_OPERATOR: "Oczekuje operatora",
    IN_PROGRESS: "W trakcie",
    COMPLETED: "Ukończone",
    CANCELLED: "Anulowane",
  };
  return labels[status] || status;
};

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text);
};

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchOrderId, setSearchOrderId] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [searchClientId, setSearchClientId] = useState("");
  const [serviceFilter, setServiceFilter] = useState("");
  const [sortBy, setSortBy] = useState<"NEWEST" | "OLDEST">("NEWEST");
  const [services, setServices] = useState<string[]>([]);

  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem("token");

      let url = `/api/admin/getOrders?page=${page}&size=20`;
      if (searchOrderId)
        url += `&order_id=${encodeURIComponent(searchOrderId)}`;
      if (statusFilter) url += `&order_status=${statusFilter}`;
      if (searchClientId)
        url += `&client_id=${encodeURIComponent(searchClientId)}`;
      if (serviceFilter) url += `&service=${encodeURIComponent(serviceFilter)}`;
      url += `&sort_by=${sortBy}`;

      console.log("🔍 Fetching orders with URL:", url);
      console.log("Filters:", {
        searchOrderId,
        statusFilter,
        searchClientId,
        serviceFilter,
        sortBy,
      });

      const response = await fetch(url, {
        method: "GET",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok)
        throw new Error(`Failed to fetch orders: ${response.status}`);

      const data = (await response.json()) as ApiResponse<Order>;
      console.log("📦 Received data:", data);
      setOrders(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setError("");
    } catch (err: unknown) {
      const error =
        err instanceof Error ? err.message : "Failed to load orders";
      console.error("❌ Error fetching orders:", error);
      setError(error);
    } finally {
      setLoading(false);
    }
  }, [
    page,
    searchOrderId,
    statusFilter,
    searchClientId,
    serviceFilter,
    sortBy,
  ]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  useEffect(() => {
    fetchServices();
  }, []);

  const handleStatusChange = (newStatus: string) => {
    setStatusFilter(newStatus);
    setPage(0);
  };

  const handleServiceChange = (newService: string) => {
    setServiceFilter(newService);
    setPage(0);
  };

  const handleOrderIdSearch = () => {
    setPage(0);
  };

  const handleClientIdSearch = () => {
    setPage(0);
  };

  const fetchServices = async () => {
    try {
      const token = localStorage.getItem("token");
      const url = `/api/services/getServices`;

      const response = await fetch(url, {
        method: "GET",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (response.ok) {
        const data = (await response.json()) as Record<string, string>;
        const serviceNames = Object.values(data);
        setServices(serviceNames);
      }
    } catch (err: unknown) {
      console.error("Failed to fetch services", err);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      return new Date(dateString).toLocaleDateString("pl-PL", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateString;
    }
  };

  if (loading) return <div className="text-gray-600">Ładowanie zleceń...</div>;

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Historia Zleceń</h1>
      {error && <div className="text-red-600 mb-5">Błąd: {error}</div>}

      <div className="mb-5 grid grid-cols-2 gap-4 md:grid-cols-4">
        <div>
          <label className="block text-sm font-semibold mb-2">
            ID Zlecenia
          </label>
          <input
            type="text"
            placeholder="Szukaj ID..."
            value={searchOrderId}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleOrderIdSearch();
              }
            }}
            onChange={(e) => setSearchOrderId(e.target.value)}
            className="w-full px-2 py-2 rounded border border-gray-300"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold mb-2">Status</label>
          <select
            value={statusFilter}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="w-full px-2 py-2 rounded border border-gray-300"
          >
            <option value="">Wszystkie statusy</option>
            <option value="OPEN">Otwarte</option>
            <option value="AWAITING_OPERATOR">Oczekuje operatora</option>
            <option value="IN_PROGRESS">W trakcie</option>
            <option value="COMPLETED">Ukończone</option>
            <option value="CANCELLED">Anulowane</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-semibold mb-2">ID Klienta</label>
          <input
            type="text"
            placeholder="Szukaj klienta..."
            value={searchClientId}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleClientIdSearch();
              }
            }}
            onChange={(e) => setSearchClientId(e.target.value)}
            className="w-full px-2 py-2 rounded border border-gray-300"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold mb-2">Usługa</label>
          <select
            value={serviceFilter}
            onChange={(e) => handleServiceChange(e.target.value)}
            className="w-full px-2 py-2 rounded border border-gray-300"
          >
            <option value="">Wszystkie usługi</option>
            {services.map((service) => (
              <option key={service} value={service}>
                {service}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mb-5 flex items-center gap-3">
        <span className="text-sm font-semibold">Sortuj:</span>
        <button
          onClick={() => setSortBy("NEWEST")}
          className={`px-4 py-2 rounded font-semibold transition-colors ${
            sortBy === "NEWEST"
              ? "bg-blue-600 text-white"
              : "bg-gray-300 text-gray-700 hover:bg-gray-400"
          }`}
        >
          ↓ Najnowsze
        </button>
        <button
          onClick={() => setSortBy("OLDEST")}
          className={`px-4 py-2 rounded font-semibold transition-colors ${
            sortBy === "OLDEST"
              ? "bg-blue-600 text-white"
              : "bg-gray-300 text-gray-700 hover:bg-gray-400"
          }`}
        >
          ↑ Najstarsze
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full border-collapse mb-5">
          <thead>
            <tr className="bg-gray-100 border-b-2 border-gray-300">
              {[
                "ID Zlecenia",
                "Tytuł",
                "Klient",
                "Operator",
                "Lokalizacja",
                "Status",
                "Usługa",
                "Data Utworzenia",
              ].map((col) => (
                <th
                  key={col}
                  className="px-2.5 py-2.5 text-left border-b border-gray-300"
                >
                  {col}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {orders.map((order: Order, index: number) => (
              <tr
                key={`${order.order_id}-${index}`}
                className="border-b border-gray-300"
              >
                <td className="px-2.5 py-2.5 text-xs">
                  <button
                    onClick={() => copyToClipboard(order.order_id)}
                    title="Kliknij aby skopiować ID"
                    className="text-blue-600 hover:text-blue-800 cursor-pointer hover:underline"
                  >
                    {String(order.order_id).substring(0, 8)}...
                  </button>
                </td>
                <td className="px-2.5 py-2.5">{order.title}</td>
                <td className="px-2.5 py-2.5 text-xs">
                  <button
                    onClick={() => copyToClipboard(order.client_id)}
                    title="Kliknij aby skopiować ID"
                    className="text-blue-600 hover:text-blue-800 cursor-pointer hover:underline"
                  >
                    {String(order.client_id).substring(0, 8)}...
                  </button>
                </td>
                <td className="px-2.5 py-2.5 text-xs">
                  <button
                    onClick={() => copyToClipboard(order.operator_id)}
                    title="Kliknij aby skopiować ID"
                    className="text-blue-600 hover:text-blue-800 cursor-pointer hover:underline"
                  >
                    {String(order.operator_id).substring(0, 8)}...
                  </button>
                </td>
                <td className="px-2.5 py-2.5">{order.coordinates}</td>
                <td className="px-2.5 py-2.5">
                  <span
                    className={`px-2 py-1 rounded ${getStatusColor(
                      order.status
                    )}`}
                  >
                    {getStatusLabel(order.status)}
                  </span>
                </td>
                <td className="px-2.5 py-2.5">
                  <span className="px-2 py-1 rounded bg-gray-200">
                    {order.service_name || "N/A"}
                  </span>
                </td>
                <td className="px-2.5 py-2.5 text-xs">
                  {formatDate(order.created_at)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {orders.length === 0 && !loading && (
        <div className="text-center py-5 text-gray-600">
          Brak zleceń do wyświetlenia
        </div>
      )}

      <div className="flex gap-2.5 justify-center items-center">
        <button
          onClick={() => setPage(Math.max(0, page - 1))}
          disabled={page === 0}
          className="px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          Poprzednia
        </button>
        <span>
          Strona {page + 1} z {totalPages}
        </span>
        <button
          onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1 || totalPages === 0}
          className="px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          Następna
        </button>
      </div>
    </div>
  );
}
