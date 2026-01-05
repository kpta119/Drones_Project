"use client";

import { useEffect, useState } from "react";
import OrderCard from "@/src/components/order_card";
import { OrderDTO } from "@/src/dto/order_dto";

export default function OrdersPage() {
    const [orders, setOrders] = useState<OrderDTO[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [location, setLocation] = useState("");
    const [radius, setRadius] = useState("");
    const [service, setService] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");

    async function fetchOrders() {
        try {
            setLoading(true);
            setError(null);
            const token = localStorage.getItem("token");
            if (!token) throw new Error("Brak tokenu JWT w localStorage");

            const params = new URLSearchParams();
            if (location) params.append("location", location);
            if (radius) params.append("radius", radius);
            if (service) params.append("service", service);
            if (fromDate) params.append("from_date", new Date(fromDate).toISOString());
            if (toDate) params.append("to_date", new Date(toDate).toISOString());

            const response = await fetch('/api/operators/getMatchedOrders?${params.toString()}', {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "X-USER-TOKEN": `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error(`Błąd pobierania: ${response.status}`);
            }

            const data = await response.json();
            setOrders(data.content || []);
        } catch (err: any) {
            setError(err.message || "Nieznany błąd");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchOrders();
    }, []);

    return (
        <div className="p-4 space-y-4">
            <div className="bg-white p-4 rounded-md shadow-md space-y-4">
                <h2 className="text-lg font-semibold">Filtruj zlecenia</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <input
                        type="text"
                        placeholder="Lokalizacja"
                        value={location}
                        onChange={(e) => setLocation(e.target.value)}
                        className="border rounded p-2 w-full"
                    />
                    <input
                        type="number"
                        placeholder="Promień (km)"
                        value={radius}
                        onChange={(e) => setRadius(e.target.value)}
                        className="border rounded p-2 w-full"
                    />
                    <input
                        type="text"
                        placeholder="Usługa"
                        value={service}
                        onChange={(e) => setService(e.target.value)}
                        className="border rounded p-2 w-full"
                    />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <label className="flex flex-col">
                        Od daty:
                        <input
                            type="date"
                            value={fromDate}
                            onChange={(e) => setFromDate(e.target.value)}
                            className="border rounded p-2 mt-1"
                        />
                    </label>
                    <label className="flex flex-col">
                        Do daty:
                        <input
                            type="date"
                            value={toDate}
                            onChange={(e) => setToDate(e.target.value)}
                            className="border rounded p-2 mt-1"
                        />
                    </label>
                </div>
                <button
                    onClick={fetchOrders}
                    className="bg-blue-600 text-white rounded-md px-4 py-2 hover:bg-blue-700"
                >
                    Szukaj
                </button>
            </div>

            {loading && <p className="text-gray-700">Ładowanie zleceń...</p>}
            {error && <p className="text-red-600">{error}</p>}
            {!loading && orders.length === 0 && !error && (
                <p className="text-gray-500">Brak dopasowanych zleceń.</p>
            )}
            <div className="space-y-4">
                {orders.map((order) => (
                    <OrderCard key={order.id} order={order} />
                ))}
            </div>
        </div>
    );
}
