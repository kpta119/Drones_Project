package com.example.drones.auth.exceptions;

import com.example.drones.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AppException {
    public UserAlreadyExistsException(String email) {
        super("User with email " + email + " already exists.", HttpStatus.CONFLICT);
    }
}
