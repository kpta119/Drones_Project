package com.example.drones.operators.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NoSuchOperatorException extends AppException {
    public NoSuchOperatorException() {
        super("No operator profile found for this user.", HttpStatus.NOT_FOUND);
    }
}
