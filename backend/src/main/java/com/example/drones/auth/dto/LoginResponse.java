package com.example.drones.auth.dto;

import com.example.drones.user.UserRole;

import java.util.UUID;

public record LoginResponse(
        String token,
        UserRole role,
        UUID userId,
        String email,
        String username

) {
}
