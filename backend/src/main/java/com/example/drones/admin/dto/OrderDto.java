package com.example.drones.admin.dto;

import com.example.drones.orders.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDto(
        @JsonProperty("order_id") UUID orderId,
        String title,
        String description,
        @JsonProperty("service_name") String serviceName,
        String coordinates,
        @JsonProperty("from_date") LocalDateTime fromDate,
        @JsonProperty("to_date") LocalDateTime toDate,
        OrderStatus status,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("client_id") UUID clientId,
        @JsonProperty("operator_id") UUID operatorId
) {
}
