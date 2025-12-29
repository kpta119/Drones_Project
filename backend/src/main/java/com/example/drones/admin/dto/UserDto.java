package com.example.drones.admin.dto;

import com.example.drones.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDto(
        String id,
        @JsonProperty("username") String displayName,
        UserRole role,
        String name,
        String surname,
        String email,
        @JsonProperty("phone_number") String phoneNumber
) {
}
