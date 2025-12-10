package com.example.drones.operators.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record OperatorDto(
        String name,
        String surname,
        String username,
        List<String> certificates,
        @JsonProperty("operator_services") List<String> operatorServices,
        String email,
        @JsonProperty("phone_number") String phoneNumber,
        OperatorPortfolioDto portfolio
) {
}
