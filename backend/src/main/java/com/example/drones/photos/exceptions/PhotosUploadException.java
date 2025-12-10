package com.example.drones.photos.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class PhotosUploadException extends AppException {
    public PhotosUploadException(String message) {
        super("Photos upload failed. " + message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
