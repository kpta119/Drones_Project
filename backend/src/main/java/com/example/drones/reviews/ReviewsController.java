package com.example.drones.reviews;

import com.example.drones.common.config.auth.JwtService;
import com.example.drones.reviews.dto.ReviewRequest;
import com.example.drones.reviews.dto.ReviewResponse;
import com.example.drones.reviews.dto.UserReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;
    private final JwtService jwtService;

    @PostMapping("/createReview/{orderId}/{targetId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID orderId,
            @PathVariable UUID targetId,
            @RequestBody @Valid ReviewRequest request
    ) {
        UUID authorId = jwtService.extractUserId();
        ReviewResponse response = reviewsService.createReview(orderId, targetId, authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getUserReviews/{userId}")
    public ResponseEntity<List<UserReviewResponse>> getUserReviews(@PathVariable UUID userId) {
        List<UserReviewResponse> response = reviewsService.getUserReviews(userId);
        return ResponseEntity.ok(response);
    }
}