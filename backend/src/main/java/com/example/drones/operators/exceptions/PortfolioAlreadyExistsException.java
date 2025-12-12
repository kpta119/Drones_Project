package com.example.drones.operators.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class PortfolioAlreadyExistsException extends AppException {
    public PortfolioAlreadyExistsException() {
        super("Portfolio already exists for this operator.", HttpStatus.CONFLICT);
    }
}
