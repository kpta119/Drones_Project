package com.example.drones.operators.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreatePortfolioDto(
        @NotBlank String title,
        @NotBlank String description
) {
}
