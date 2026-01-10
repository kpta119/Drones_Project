package com.example.drones.services.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class ServiceNotFoundException extends AppException {
    public ServiceNotFoundException() {
        super("Service not found", HttpStatus.NOT_FOUND);
    }
}
