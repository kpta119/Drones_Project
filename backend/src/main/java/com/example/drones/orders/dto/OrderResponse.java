package com.example.drones.orders.dto;

import com.example.drones.orders.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;

    private String title;
    private String description;
    private String service;
    private Map<String, String> parameters;
    private String coordinates;

    @JsonProperty("from_date")
    private LocalDateTime fromDate;

    @JsonProperty("to_date")
    private LocalDateTime toDate;

    private OrderStatus status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}