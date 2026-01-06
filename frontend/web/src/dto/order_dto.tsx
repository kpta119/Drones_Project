import { ISODate, UUID } from "./common";

export type OrderStatus =
    | "open"
    | "awaiting_operator"
    | "in_progress"
    | "completed"
    | "cancelled";

export interface CreateOrderRequestDTO {
    title: string;
    description: string;
    service: string;
    parameters: Record<string, string>;
    coordinates: string;
    from_date: ISODate;
    to_date: ISODate;
}

export interface OrderDTO {
    id: number | UUID;
    title: string;
    clientId: UUID;
    description: string;
    service: string;
    parameters: Record<string, string>;
    coordinates: string;
    from_date: ISODate;
    to_date: ISODate;
    status: OrderStatus;
    created_at: ISODate;
    operator_id: number | null;
}
