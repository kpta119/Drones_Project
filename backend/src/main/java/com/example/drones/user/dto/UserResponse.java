package com.example.drones.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String username;
    private String name;
    private String surname;
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;
}