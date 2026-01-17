package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OrderAlreadyAcceptedByYouException extends AppException {
    public OrderAlreadyAcceptedByYouException() {
        super("You have already accepted this order.", HttpStatus.BAD_REQUEST);
    }
}

