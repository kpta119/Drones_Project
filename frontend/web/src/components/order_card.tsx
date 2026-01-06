import { OrderDTO } from "@/src/dto/order_dto";

interface OrderCardProps {
    order: OrderDTO;
}

export default function OrderCard({ order }: OrderCardProps) {
    return (
        <div className="w-full rounded-md border p-4 shadow-sm">
            <h2 className="text-lg font-semibold">{order.title}</h2>

            <p className="text-sm text-gray-600">
                Usługa: <span className="font-medium">{order.service}</span>
            </p>

            <p className="text-sm text-gray-500">
                Do: {new Date(order.to_date).toLocaleString()}
            </p>
        </div>
    );
}