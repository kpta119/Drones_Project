package com.example.drones.auth.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class UserAccountLockedException extends AppException {
    public UserAccountLockedException() {

        super("Account has been banned", HttpStatus.FORBIDDEN);
    }
}
