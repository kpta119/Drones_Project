package com.example.drones.operators.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record MatchingOperatorDto(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("username") String displayName,
        String name,
        String surname,
        List<String> certificates
) {
}
