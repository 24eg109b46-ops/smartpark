package com.smartpark.controller.api;

import com.smartpark.dto.VehicleDto;
import com.smartpark.entity.Vehicle;
import com.smartpark.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleApiController {

    private final VehicleService vehicleService;

    public VehicleApiController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserVehicles(@PathVariable("userId") Long userId) {
        List<Vehicle> list = vehicleService.getVehiclesByUserId(userId);
        List<Map<String, Object>> result = list.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("vehicleNumber", v.getVehicleNumber());
            map.put("vehicleType", v.getVehicleType().name());
            map.put("brand", v.getBrand());
            map.put("model", v.getModel());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> addVehicle(@PathVariable("userId") Long userId, @Valid @RequestBody VehicleDto dto) {
        try {
            Vehicle vehicle = vehicleService.addVehicle(userId, dto);
            return ResponseEntity.ok(Map.of("id", vehicle.getId(), "vehicleNumber", vehicle.getVehicleNumber(), "message", "Vehicle added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        try {
            vehicleService.deleteVehicle(userId, id);
            return ResponseEntity.ok(Map.of("message", "Vehicle deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
