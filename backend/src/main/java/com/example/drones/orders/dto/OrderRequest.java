package com.example.drones.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class OrderRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Service name is required")
    private String service;

    private Map<String, String> parameters;

    @NotBlank(message = "Coordinates are required")
    private String coordinates;

    @NotNull
    @Future(message = "From date must be in the future")
    @JsonProperty("from_date")
    private LocalDateTime fromDate;

    @NotNull
    @Future(message = "To date must be in the future")
    @JsonProperty("to_date")
    private LocalDateTime toDate;
}