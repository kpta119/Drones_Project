package com.example.drones.reviews.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class ReviewAlreadyExistsException extends AppException {
    public ReviewAlreadyExistsException() {
        super("The review to this order was already published.", HttpStatus.BAD_REQUEST);
    }
}
