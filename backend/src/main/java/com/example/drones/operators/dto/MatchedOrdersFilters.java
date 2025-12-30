package com.example.drones.operators.dto;

import com.example.drones.orders.MatchedOrderStatus;
import com.example.drones.orders.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record MatchedOrdersFilters(
        String location,
        Integer radius,
        String service,
        @JsonProperty("from_date") LocalDateTime fromDate,
        @JsonProperty("to_date") LocalDateTime toDate,
        @JsonProperty("order_status") OrderStatus orderStatus,
        @JsonProperty("client_status") MatchedOrderStatus clientStatus,
        @JsonProperty("operator_status") MatchedOrderStatus operatorStatus
) {
}
