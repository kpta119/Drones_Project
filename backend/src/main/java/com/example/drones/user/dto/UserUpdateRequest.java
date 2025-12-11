package com.example.drones.user.dto;

import com.example.drones.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    private String name;
    private String surname;
    private String username;
    private UserRole role;

    @JsonProperty("phone_number")
    private String phoneNumber;

    private String coordinates;
    private Integer radius;
    private List<String> certificates;
}
