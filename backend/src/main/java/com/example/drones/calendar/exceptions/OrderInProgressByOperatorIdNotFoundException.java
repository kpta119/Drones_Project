package com.example.drones.calendar.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OrderInProgressByOperatorIdNotFoundException extends AppException {
    public OrderInProgressByOperatorIdNotFoundException() {
        super("Order in progress by operator ID not found", HttpStatus.NOT_FOUND);
    }
}
