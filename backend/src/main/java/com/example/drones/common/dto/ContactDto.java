package com.example.drones.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ContactDto(
        @NotNull @Email String email,
        @NotNull String phoneNumber
) {
}