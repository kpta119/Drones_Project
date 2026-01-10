package com.example.drones.reviews.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private UUID orderId;
    private UUID authorId;
    private UUID targetId;
    private int stars;
    private String body;
}