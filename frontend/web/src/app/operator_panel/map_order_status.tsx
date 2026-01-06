import { OrderStatus } from "@/src/dto/order_dto";

export function mapOrderStatus(apiStatus: string): OrderStatus {
    switch (apiStatus) {
        case "OPEN":
            return "open";
        case "PENDING":
            return "awaiting_operator";
        case "IN_PROGRESS":
            return "in_progress";
        case "COMPLETED":
            return "completed";
        case "CANCELLED":
            return "cancelled";
        default:
            return "open";
    }
}
