package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class MatchedOrderNotFoundException extends AppException {
    public MatchedOrderNotFoundException() {
        super("Matched order not found", HttpStatus.NOT_FOUND);
    }
}
