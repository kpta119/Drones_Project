package com.example.drones.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FirstController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Drones API");
        response.put("version", "1.0.0");
        
        System.out.println("Health check requested at: " + LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/drones/count")
    public ResponseEntity<Map<String, Object>> getDronesCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalDrones", 5);
        response.put("activeDrones", 3);
        response.put("inactiveDrones", 2);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}