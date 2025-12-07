package com.example.drones.operators.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;


@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "DTO class with short lifecycle")
@Builder
public record CreateOperatorDto(
        @NotBlank
        String coordinates,
        @NotNull
        Integer radius,
        @NotNull
        List<String> certificates,
        @NotEmpty
        List<String> services
) {
}
