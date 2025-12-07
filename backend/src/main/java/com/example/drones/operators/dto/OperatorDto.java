package com.example.drones.operators.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;


@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "DTO class with short lifecycle")
@Builder
public record OperatorDto(
        @NotNull @NotEmpty String coordinates,
        @NotNull Integer radius,
        List<String> certificates,
        @NotEmpty List<String> services
) {
}
