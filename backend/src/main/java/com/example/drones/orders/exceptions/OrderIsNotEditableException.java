package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OrderIsNotEditableException extends AppException {
    public OrderIsNotEditableException() {
        super("Order is not editable because of status", HttpStatus.FORBIDDEN);
    }
}
