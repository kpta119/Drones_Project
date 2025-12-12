package com.example.drones.services;

import com.example.drones.services.ServicesService;
import com.example.drones.services.dto.ServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServicesController {

    private final ServicesService servicesService;

    @GetMapping("/getServices")
    public ResponseEntity<List<String>> getServices() {
        return ResponseEntity.ok(servicesService.getAllServices());
    }

    @PostMapping
    public ResponseEntity<List<String>> addServices(@RequestBody List<String> serviceNames) {
        List<String> createdServices = servicesService.addServices(serviceNames);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdServices);
    }
}