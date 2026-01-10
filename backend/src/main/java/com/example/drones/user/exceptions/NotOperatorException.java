package com.example.drones.user.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NotOperatorException extends AppException {
    public NotOperatorException() {
        super("You are not operator but you should be to accept the offer", HttpStatus.FORBIDDEN);
    }
}
