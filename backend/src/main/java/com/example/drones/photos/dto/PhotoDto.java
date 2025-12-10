package com.example.drones.photos.dto;

import lombok.Builder;

@Builder
public record PhotoDto(
        Integer id,
        String name,
        String url
) {
}
