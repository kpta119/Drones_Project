package com.example.drones.photos.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

import java.util.List;

@Builder
@SuppressFBWarnings({"EI_EXPOSE_REP"})
public record PhotosDto(
        List<PhotoDto> photos
) {
}
