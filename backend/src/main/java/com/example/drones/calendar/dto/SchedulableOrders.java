package com.example.drones.calendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulableOrders {

    private UUID id;
    @JsonProperty("is_already_added")
    private Boolean alreadyAdded;
    @JsonProperty("client_id")
    private UUID clientId;
    private String title;
    private String description;
    private String service;
    private Map<String, String> parameters;
    private String coordinates;
    @JsonProperty("from_date")
    private LocalDateTime fromDate;

    @JsonProperty("to_date")
    private LocalDateTime toDate;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
