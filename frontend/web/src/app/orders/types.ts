export type OrderStatus =
  | "OPEN"
  | "AWAITING_OPERATOR"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";
export type MatchStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export const OrderStatusLabels: Record<OrderStatus, string> = {
  OPEN: "Otwarte",
  AWAITING_OPERATOR: "Dobieranie operatora",
  IN_PROGRESS: "W trakcie realizacji",
  COMPLETED: "Zakończone",
  CANCELLED: "Anulowane",
};

export interface OrderResponse {
  id: string;
  client_id: string;
  title: string;
  description: string;
  service: string;
  coordinates: string;
  from_date: string;
  to_date: string;
  status: OrderStatus;
  created_at: string;
  parameters: Record<string, string>;
  operator_id?: string;
}

export interface MatchedOrderDto {
  id: string;
  client_id: string;
  title: string;
  description: string;
  service: string;
  parameters: Record<string, string>;
  coordinates: string;
  distance: number;
  from_date: string;
  to_date: string;
  created_at: string;
  order_status: OrderStatus;
  client_status: MatchStatus;
  operator_status: MatchStatus;
}

export interface OperatorApplicantDto {
  user_id: string;
  username: string;
  name: string;
  surname: string;
  certificates: string[];
  rating?: number;
  description?: string;
}
