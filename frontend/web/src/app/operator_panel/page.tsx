"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { OrderDTO, OrderStatus } from "@/src/dto/order_dto";
import { getMatchedOrders } from "./operator_orders";
import OrderCard from "@/src/components/order_card";

const STATUS_LABELS: Record<OrderStatus, string> = {
    open: "Otwarte",
    awaiting_operator: "Oczekujące na operatora",
    in_progress: "W trakcie realizacji",
    completed: "Zakończone",
    cancelled: "Anulowane",
};

const VISIBLE_STATUSES: OrderStatus[] = [
    "awaiting_operator",
    "in_progress",
    "completed",
];

export default function OrdersPage() {
    const router = useRouter();

    const [orders, setOrders] = useState<OrderDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const token = localStorage.getItem("token");

        if (!token) {
            setError("Brak tokenu JWT w localStorage");
            setLoading(false);
            return;
        }

        getMatchedOrders(token)
            .then(setOrders)
            .catch(() => setError("Błąd pobierania zleceń"))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return <p className="p-4">Ładowanie...</p>;
    }

    if (error) {
        return <p className="p-4 text-red-600">{error}</p>;
    }

    const groupedOrders = orders.reduce<Record<OrderStatus, OrderDTO[]>>(
        (acc, order) => {
            acc[order.status].push(order);
            return acc;
        },
        {
            open: [],
            awaiting_operator: [],
            in_progress: [],
            completed: [],
            cancelled: [],
        }
    );

    return (
        <div className="space-y-8 p-6">
            {/* Przycisk nawigacji */}
            <div>
                <button
                    onClick={() => router.push("/orders")}
                    className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
                >
                    Znajdź nowe zlecenie
                </button>
            </div>

            {VISIBLE_STATUSES.map(status => (
                <section key={status}>
                    <h1 className="mb-4 text-xl font-bold">
                        {STATUS_LABELS[status]}
                    </h1>

                    {groupedOrders[status].length === 0 ? (
                        <p className="text-sm text-gray-500">
                            Brak zleceń w tej kategorii
                        </p>
                    ) : (
                        <div className="grid gap-4">
                            {groupedOrders[status].map(order => (
                                <OrderCard
                                    key={order.id.toString()}
                                    order={order}
                                />
                            ))}
                        </div>
                    )}
                </section>
            ))}
        </div>
    );
}
