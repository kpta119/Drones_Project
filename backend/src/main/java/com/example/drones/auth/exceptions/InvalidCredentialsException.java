package com.example.drones.auth.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("The provided credentials are invalid.");
    }
}
