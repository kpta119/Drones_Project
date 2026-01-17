package com.example.drones.calendar.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class AddEventToCalendarException extends AppException {
    public AddEventToCalendarException() {
        super("Failed to add event to calendar", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
