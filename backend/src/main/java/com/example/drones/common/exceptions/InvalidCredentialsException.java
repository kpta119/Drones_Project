package com.example.drones.common.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("The provided credentials are invalid.");
    }
}
