package com.example.drones.operators.dto;

import lombok.Builder;

@Builder
public record UpdatePortfolioDto(
        String title,
        String description
) {
}
