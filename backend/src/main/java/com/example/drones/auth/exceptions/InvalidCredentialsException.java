package com.example.drones.auth.exceptions;

import com.example.drones.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException() {
        super("The provided credentials are invalid.", HttpStatus.UNAUTHORIZED);
    }
}
