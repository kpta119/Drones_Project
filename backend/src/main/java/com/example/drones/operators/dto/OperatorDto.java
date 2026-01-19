package com.example.drones.operators.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

import java.util.List;

@Builder
@SuppressFBWarnings({"EI_EXPOSE_REP"})
public record OperatorDto(
        String name,
        String surname,
        String username,
        List<String> certificates,
        @JsonProperty("operator_services") List<String> operatorServices,
        String email,
        String coordinates,
        Integer radius,
        @JsonProperty("average_stars") Double averageStars,
        @JsonProperty("phone_number") String phoneNumber,
        OperatorPortfolioDto portfolio
) {
}
