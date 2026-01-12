package com.example.drones.calendar.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class UserIsNotConnectedToGoogleException extends AppException {
    public UserIsNotConnectedToGoogleException() {

        super("User is not connected to Google", HttpStatus.BAD_REQUEST);
    }
}
