package com.example.drones.operators.dto;

import com.example.drones.photos.dto.PhotoDto;
import lombok.Builder;

import java.util.List;

@Builder
public record OperatorPortfolioDto(
        String title,
        String description,
        List<PhotoDto> photos
) {
}
