package com.example.drones.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class OrderUpdateRequest {

    private String title;
    private String description;

    private String service;

    private Map<String, String> parameters;
    private String coordinates;

    @JsonProperty("from_date")
    @Future
    private LocalDateTime fromDate;

    @JsonProperty("to_date")
    @Future
    private LocalDateTime toDate;
}