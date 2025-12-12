package com.example.drones.photos.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PhotosDto(
        List<PhotoDto> photos
) {
}
