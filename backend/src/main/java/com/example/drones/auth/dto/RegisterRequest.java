package com.example.drones.auth.dto;

import com.example.drones.common.dto.ContactDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest (
        @NotNull String username,
        @NotNull String password,
        @NotNull String name,
        @NotNull String surname,
        @NotNull @Valid ContactDto contact
) {}
