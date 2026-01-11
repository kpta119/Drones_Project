package com.example.drones.calendar;

import com.example.drones.calendar.dto.SchedulableOrders;
import com.example.drones.common.config.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/getInProgressSchedulableOrders")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<Page<SchedulableOrders>> getSchedulableOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID userId = jwtService.extractUserId();
        Page<SchedulableOrders> orders = calendarService.getInProgressSchedulableOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }
}
