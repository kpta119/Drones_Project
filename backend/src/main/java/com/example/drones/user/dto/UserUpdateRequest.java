package com.example.drones.user.dto;

import com.example.drones.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;

import java.util.List;

@Builder
@SuppressFBWarnings({"EI_EXPOSE_REP"})
public record UserUpdateRequest(
        String name,
        String surname,
        String username,
        UserRole role,


        @JsonProperty("phone_number")
        String phoneNumber,

        String coordinates,
        Integer radius,
        List<String> certificates
) {
}
