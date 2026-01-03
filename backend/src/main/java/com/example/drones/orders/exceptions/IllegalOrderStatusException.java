package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class IllegalOrderStatusException extends AppException {
    public IllegalOrderStatusException(String statusStr) {
        super("You passed incorrect order status: " + statusStr, HttpStatus.BAD_REQUEST);
    }
}
