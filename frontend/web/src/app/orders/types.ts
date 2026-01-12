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
  title: string;
  description: string;
  service: string;
  coordinates: string;
  from_date: string;
  to_date: string;
  status: OrderStatus;
  client_id: string;
  createdAt: string;
  parameters: Record<string, string>;
  city?: string;
  street?: string;
}

export interface MatchedOrderDto {
  id: string;
  clientId: string;
  title: string;
  description: string;
  service: string;
  parameters: Record<string, string>;
  coordinates: string;
  fromDate: string;
  toDate: string;
  createdAt: string;
  orderStatus: OrderStatus;
  distance: number;
  clientStatus: MatchStatus;
  operatorStatus: MatchStatus;
}

export interface ActiveOrderDto extends OrderResponse {
  startDate: string;
  endDate: string;
}

export interface OperatorApplicantDto {
  id: string;
  name: string;
  surname: string;
  username: string;
  rating: number;
  photoUrl?: string;
  description: string;
}
