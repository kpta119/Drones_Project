package com.example.drones.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReviewResponse {
    private String name;
    private String surname;
    private String username;
    private int stars;
    private String body;
}