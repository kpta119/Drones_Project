package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class CannotAcceptOwnOrderException extends AppException {
    public CannotAcceptOwnOrderException() {
        super("You cannot accept your own order.", HttpStatus.FORBIDDEN);
    }
}
