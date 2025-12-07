package com.example.drones.operators.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

import java.util.List;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "DTO class with short lifecycle")
@Builder
public record OperatorProfileDto(
        String coordinates,
        Integer radius,
        List<String> certificates,
        List<String> services
) {
}
