package com.example.drones.reviews.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class UnauthorizedReviewException extends AppException {
    public UnauthorizedReviewException() {
        super("You are not authorized to review this order. Only the order owner or assigned operator can review a completed order.", HttpStatus.FORBIDDEN);
    }
}

