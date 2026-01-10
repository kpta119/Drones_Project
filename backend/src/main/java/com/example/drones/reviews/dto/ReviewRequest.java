package com.example.drones.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewRequest {
    @Min(1) @Max(5)
    private int stars;
    private String body;
}