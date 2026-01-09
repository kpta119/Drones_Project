package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OrderAlreadyHasAcceptedOperatorException extends AppException {
    public OrderAlreadyHasAcceptedOperatorException() {
        super("Operator has been already chosen for this order.",HttpStatus.FORBIDDEN);
    }
}
