package com.example.drones.operators.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class OperatorAlreadyExistsException extends AppException {
    public OperatorAlreadyExistsException() {
        super("Operator profile already exists for this user.", HttpStatus.CONFLICT);
    }
}