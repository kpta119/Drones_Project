package com.example.drones.config.exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        int statusCode,
        LocalDateTime timestamp
) {}