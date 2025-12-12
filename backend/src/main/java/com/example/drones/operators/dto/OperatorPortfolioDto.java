package com.example.drones.operators.dto;

import com.example.drones.photos.dto.PhotoDto;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

import java.util.List;

@Builder
@SuppressFBWarnings({"EI_EXPOSE_REP"})
public record OperatorPortfolioDto(
        String title,
        String description,
        List<PhotoDto> photos
) {
}
