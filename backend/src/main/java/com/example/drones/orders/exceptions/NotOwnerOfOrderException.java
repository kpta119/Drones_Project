package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NotOwnerOfOrderException extends AppException {
    public NotOwnerOfOrderException() {
        super("You are not owner of the order", HttpStatus.FORBIDDEN);
    }
}
