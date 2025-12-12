package com.example.drones.services.exceptions;

import com.example.drones.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class ServiceAlreadyExistsException extends AppException {
    public ServiceAlreadyExistsException(String name) {
        super("Service with name " + name + " already exists.", HttpStatus.CONFLICT);
    }
}
