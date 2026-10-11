package com.smartpark.controller.api;

import com.smartpark.dto.BulkSlotCreateRequest;
import com.smartpark.dto.SlotDto;
import com.smartpark.dto.SlotSearchRequest;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.entity.ParkingZone;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.service.ParkingSlotService;
import com.smartpark.service.ParkingZoneService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "*")
public class SlotApiController {

    private final ParkingSlotService parkingSlotService;
    private final ParkingZoneService parkingZoneService;

    public SlotApiController(ParkingSlotService parkingSlotService,
                             ParkingZoneService parkingZoneService) {
        this.parkingSlotService = parkingSlotService;
        this.parkingZoneService = parkingZoneService;
    }

    @GetMapping("/zones")
    public ResponseEntity<List<Map<String, Object>>> getZones() {
        List<ParkingZone> zones = parkingZoneService.getActiveZones();
        List<Map<String, Object>> result = zones.stream().map(z -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", z.getId());
            map.put("name", z.getName());
            map.put("location", z.getLocation());
            map.put("floor", z.getFloor());
            map.put("capacity", z.getCapacity());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSlots(
            @RequestParam(value = "zoneId", required = false) Long zoneId,
            @RequestParam(value = "vehicleType", required = false) VehicleType vehicleType,
            @RequestParam(value = "entryTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entryTime,
            @RequestParam(value = "durationHours", defaultValue = "2") Integer durationHours) {

        SlotSearchRequest request = new SlotSearchRequest();
        request.setZoneId(zoneId);
        request.setVehicleType(vehicleType);
        request.setEntryTime(entryTime != null ? entryTime : LocalDateTime.now().plusMinutes(5));
        request.setDurationHours(durationHours);

        List<ParkingSlot> slots = parkingSlotService.searchAvailableSlots(request);
        List<Map<String, Object>> result = slots.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("slotNumber", s.getSlotNumber());
            map.put("vehicleType", s.getVehicleType().name());
            map.put("zoneId", s.getZone().getId());
            map.put("zoneName", s.getZone().getName());
            map.put("zoneLocation", s.getZone().getLocation());
            map.put("zoneFloor", s.getZone().getFloor());
            map.put("status", s.getStatus().name());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<Map<String, Object>>> getSlotsByZone(@PathVariable("zoneId") Long zoneId) {
        List<ParkingSlot> slots = parkingSlotService.getSlotsByZone(zoneId);
        List<Map<String, Object>> result = slots.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("slotNumber", s.getSlotNumber());
            map.put("vehicleType", s.getVehicleType().name());
            map.put("status", s.getStatus().name());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createSlot(@Valid @RequestBody SlotDto dto) {
        try {
            ParkingSlot slot = parkingSlotService.createSlot(dto);
            return ResponseEntity.ok(Map.of("id", slot.getId(), "slotNumber", slot.getSlotNumber(), "message", "Slot created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> bulkCreateSlots(@Valid @RequestBody BulkSlotCreateRequest request) {
        try {
            int count = parkingSlotService.bulkCreateSlots(request);
            return ResponseEntity.ok(Map.of("count", count, "message", "Successfully created " + count + " slots"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateSlotStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        try {
            SlotStatus status = SlotStatus.valueOf(body.get("status"));
            ParkingSlot slot = parkingSlotService.updateSlotStatus(id, status);
            return ResponseEntity.ok(Map.of("id", slot.getId(), "status", slot.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable("id") Long id) {
        try {
            parkingSlotService.deleteSlot(id);
            return ResponseEntity.ok(Map.of("message", "Slot deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
