package com.example.drones.orders.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends AppException {
    public OrderNotFoundException() {super("Order not found", HttpStatus.NOT_FOUND);}
}
