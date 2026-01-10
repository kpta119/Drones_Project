package com.example.drones.reviews.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class IllegalOrderStateToReviewException extends AppException {
    public IllegalOrderStateToReviewException() {
        super("You can only review the order that is completed or in progress", HttpStatus.BAD_REQUEST);
    }
}
