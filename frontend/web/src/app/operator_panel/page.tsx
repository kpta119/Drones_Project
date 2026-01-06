"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { OrderDTO, OrderStatus } from "@/src/dto/order_dto";
import { getMatchedOrders } from "./operator_orders";
import OrderCard from "@/src/components/order_card";

type ViewState =
    | { status: "loading" }
    | { status: "error"; message: string }
    | { status: "ready"; orders: OrderDTO[] };

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

export default function OperatorPanelPage() {
    const router = useRouter();
    const [viewState, setViewState] = useState<ViewState>({ status: "loading" });

    useEffect(() => {
        const fetchOrdersAsync = async () => {
            const token = localStorage.getItem("token");
            if (!token) {
                setViewState({
                    status: "error",
                    message: "Brak tokenu JWT w localStorage",
                });
                return;
            }
            try {
                const orders: OrderDTO[] = await getMatchedOrders(token);
                setViewState({ status: "ready", orders });
            } catch {
                setViewState({
                    status: "error",
                    message: "Błąd pobierania zleceń",
                });
            }
        };

        void fetchOrdersAsync(); // wywołanie asynchroniczne bez synchronizacji
    }, []); // brak dependency na fetchOrders

    if (viewState.status === "loading") {
        return <p className="p-4">Ładowanie...</p>;
    }

    if (viewState.status === "error") {
        return <p className="p-4 text-red-600">{viewState.message}</p>;
    }

    const groupedOrders: Record<OrderStatus, OrderDTO[]> = {
        open: [],
        awaiting_operator: [],
        in_progress: [],
        completed: [],
        cancelled: [],
    };

    for (const order of viewState.orders) {
        groupedOrders[order.status].push(order);
    }

    return (
        <div className="space-y-8 p-6">
            <button
                onClick={() => router.push("/orders")}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
                Znajdź nowe zlecenie
            </button>

            {VISIBLE_STATUSES.map((status) => (
                <section key={status}>
                    <h1 className="mb-4 text-xl font-bold">{STATUS_LABELS[status]}</h1>

                    {groupedOrders[status].length === 0 ? (
                        <p className="text-sm text-gray-500">
                            Brak zleceń w tej kategorii
                        </p>
                    ) : (
                        <div className="grid gap-4">
                            {groupedOrders[status].map((order) => (
                                <OrderCard key={order.id.toString()} order={order} />
                            ))}
                        </div>
                    )}
                </section>
            ))}
        </div>
    );
}