package com.example.drones.admin.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NoSuchUserException extends AppException {
    public NoSuchUserException() {
        super("No such user exists", HttpStatus.NOT_FOUND);
    }
}
