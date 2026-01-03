package com.example.drones.operators.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;


@SuppressFBWarnings(value = {"EI_EXPOSE_REP"}, justification = "DTO class with short lifecycle")
@Builder
public record CreateOperatorProfileDto(
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
