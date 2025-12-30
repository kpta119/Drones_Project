package com.example.drones.operators.dto;

import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record MatchedOrderDto(
        UUID id,
        @JsonProperty("client_id") UUID clientId,
        String title,
        String description,
        String service,
        Map<String, String> parameters,
        String coordinates,
        Double distance,
        @JsonProperty("from_date") LocalDateTime fromDate,
        @JsonProperty("to_date") LocalDateTime toDate,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("order_status") OrderStatus orderStatus,
        @JsonProperty("client_status") MatchedOrderStatus clientStatus,
        @JsonProperty("operator_status") MatchedOrderStatus operatorStatus
) {
}
