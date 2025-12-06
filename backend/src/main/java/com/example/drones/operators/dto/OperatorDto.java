package com.example.drones.operators.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record OperatorDto(
        @NotNull @NotEmpty String coordinates,
        @NotNull Integer radius,
        List<String> certificates,
        @NotEmpty List<String> services
) {
}
