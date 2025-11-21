package com.example.drones.common;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        int statusCode,
        LocalDateTime timestamp
) {}