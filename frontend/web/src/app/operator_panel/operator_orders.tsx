// src/services/operatorOrders.ts
import { OrderDTO } from "@/src/dto/order_dto";
import { mapOrderStatus } from "./map_order_status";

interface ApiOrder {
    id: string;
    title: string;
    description: string;
    service: string;
    parameters: Record<string, string>;
    coordinates: string;
    from_date: string;
    to_date: string;
    created_at: string;
    order_status: string;
    client_id: string;
}

export async function getMatchedOrders(token: string): Promise<OrderDTO[]> {
    const res = await fetch(
        "/api/operators/getMatchedOrders",
        {
            headers: {
                "Content-Type": "application/json",
                "X-USER-TOKEN": `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error("Nie udało się pobrać zleceń");
    }

    const data = await res.json();

    return data.content.map((order: ApiOrder): OrderDTO => ({
        id: order.id,
        title: order.title,
        description: order.description,
        service: order.service,
        parameters: order.parameters,
        coordinates: order.coordinates,
        from_date: order.from_date,
        to_date: order.to_date,
        created_at: order.created_at,
        clientId: order.client_id,
        status: mapOrderStatus(order.order_status),
        operator_id: null,
    }));
}
