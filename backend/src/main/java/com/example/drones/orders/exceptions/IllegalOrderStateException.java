package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class IllegalOrderStateException extends AppException {
    public IllegalOrderStateException() {
        super("You cannot cancel the order that is already completed", HttpStatus.BAD_REQUEST);
    }

    public IllegalOrderStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
