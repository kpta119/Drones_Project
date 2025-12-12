package com.example.drones.operators.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NoSuchPortfolioException extends AppException {
    public NoSuchPortfolioException() {
        super("Operator portfolio not found", HttpStatus.NOT_FOUND);
    }
}
