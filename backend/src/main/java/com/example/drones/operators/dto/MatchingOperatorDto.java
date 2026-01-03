package com.example.drones.operators.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.UUID;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "DTO class with short lifecycle")
public record MatchingOperatorDto(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("username") String displayName,
        String name,
        String surname,
        List<String> certificates
) {
}
