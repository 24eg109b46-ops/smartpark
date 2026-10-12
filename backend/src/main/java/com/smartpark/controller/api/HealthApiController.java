package com.smartpark.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HealthApiController {

    @GetMapping({"/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "SmartPark REST Backend");
        status.put("version", "1.0.0");
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }
}