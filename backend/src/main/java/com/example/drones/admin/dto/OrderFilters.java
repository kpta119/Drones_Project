package com.example.drones.admin.dto;

import com.example.drones.orders.OrderStatus;
import org.springframework.web.bind.annotation.BindParam;

import java.util.UUID;

public record OrderFilters(
        @BindParam("order_id") UUID orderId,
        @BindParam("client_id") UUID clientId,
        @BindParam("order_status") OrderStatus orderStatus,
        String service,
        @BindParam("sort_by") SortBy sortBy
) {
}
