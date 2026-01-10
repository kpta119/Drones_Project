package com.example.drones.reviews.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class IllegalTargetOfReviewException extends AppException {
    public IllegalTargetOfReviewException() {
        super("You cannot review yourself.", HttpStatus.BAD_REQUEST);
    }
}
