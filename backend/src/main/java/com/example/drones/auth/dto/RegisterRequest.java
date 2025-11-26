package com.example.drones.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull @JsonProperty("username") String displayName,
        @NotNull String password,
        @NotNull String name,
        @NotNull String surname,
        @NotNull @Email String email,
        @NotNull @JsonProperty("phone_number") String phoneNumber
) {
}
