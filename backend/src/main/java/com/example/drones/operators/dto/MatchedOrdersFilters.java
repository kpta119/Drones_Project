package com.example.drones.operators.dto;

import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.OrderStatus;

import java.time.LocalDateTime;

public record MatchedOrdersFilters(
        String location,
        Integer radius,
        String service,
        LocalDateTime from_date,
        LocalDateTime to_date,
        OrderStatus order_status,
        MatchedOrderStatus client_status,
        MatchedOrderStatus operator_status
) {
}
