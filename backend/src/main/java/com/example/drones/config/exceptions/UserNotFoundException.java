package com.example.drones.config.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String identifier) {
        super("User with identifier " + identifier + " not found", HttpStatus.NOT_FOUND);
    }
}
