package com.example.drones.calendar;

import com.example.drones.common.config.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final JwtService jwtService;

    @PostMapping("/addEvent/{orderId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<String> addEvent(@PathVariable UUID orderId) {
        UUID userId = jwtService.extractUserId();
        String link = calendarService.addEvent(userId, orderId);
        return ResponseEntity.ok(link);
    }
}
