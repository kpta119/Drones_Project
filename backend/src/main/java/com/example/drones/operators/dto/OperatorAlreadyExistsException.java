package com.example.drones.operators.dto;

import com.example.drones.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OperatorAlreadyExistsException extends AppException {
    public OperatorAlreadyExistsException() {
        super("Operator profile already exists for this user.", HttpStatus.CONFLICT);
    }
}