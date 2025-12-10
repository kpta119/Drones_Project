package com.example.drones.photos.exceptions;

import com.example.drones.common.config.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class PhotosUploadValidationException extends AppException {
    public PhotosUploadValidationException() {
        super("Number of images and names must be equal", HttpStatus.BAD_REQUEST);
    }
}
