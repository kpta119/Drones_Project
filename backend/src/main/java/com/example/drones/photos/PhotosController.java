package com.example.drones.photos;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.photos.dto.PhotosDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/photos")
public class PhotosController {

    private final PhotosService photosService;
    private final JwtService jwtService;

    @PostMapping("/addPortfolioPhotos")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<PhotosDto> addPhotos(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("names") List<String> names
    ) {
        UUID userId = jwtService.extractUserId();
        PhotosDto response = photosService.addPhotos(userId, images, names);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/deletePhotos")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Void> deletePhotos(@RequestBody List<Integer> photoIds) {
        UUID userId = jwtService.extractUserId();
        photosService.deletePhotos(userId, photoIds);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
