package com.smartpark.controller.api;

import com.smartpark.dto.DashboardStatsDto;
import com.smartpark.dto.PricingRuleDto;
import com.smartpark.dto.RegisterRequest;
import com.smartpark.entity.AuditLog;
import com.smartpark.entity.User;
import com.smartpark.enums.Role;
import com.smartpark.service.AuditLogService;
import com.smartpark.service.PricingService;
import com.smartpark.service.ReportService;
import com.smartpark.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminApiController {

    private final ReportService reportService;
    private final UserService userService;
    private final PricingService pricingService;
    private final AuditLogService auditLogService;

    public AdminApiController(ReportService reportService,
                              UserService userService,
                              PricingService pricingService,
                              AuditLogService auditLogService) {
        this.reportService = reportService;
        this.userService = userService;
        this.pricingService = pricingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(@RequestParam(value = "role", required = false) Role role) {
        Page<User> page = (role != null) ?
                userService.getUsersByRole(role, PageRequest.of(0, 100, Sort.by("createdAt").descending())) :
                userService.getAllUsers(PageRequest.of(0, 100, Sort.by("createdAt").descending()));

        List<Map<String, Object>> result = page.getContent().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRole().name());
            map.put("status", u.getStatus().name());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/operators")
    public ResponseEntity<?> createOperator(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createOperator(request);
            return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail(), "message", "Operator created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable("id") Long id) {
        try {
            User user = userService.toggleUserStatus(id);
            return ResponseEntity.ok(Map.of("id", user.getId(), "status", user.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pricing")
    public ResponseEntity<List<PricingRuleDto>> getPricingRules() {
        return ResponseEntity.ok(pricingService.getAllRules());
    }

    @PostMapping("/pricing")
    public ResponseEntity<?> createPricingRule(@Valid @RequestBody PricingRuleDto dto) {
        try {
            PricingRuleDto created = pricingService.createRule(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/pricing/{id}")
    public ResponseEntity<?> updatePricingRule(@PathVariable("id") Long id, @Valid @RequestBody PricingRuleDto dto) {
        try {
            PricingRuleDto updated = pricingService.updateRule(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/pricing/{id}")
    public ResponseEntity<?> deletePricingRule(@PathVariable("id") Long id) {
        try {
            pricingService.deleteRule(id);
            return ResponseEntity.ok(Map.of("message", "Pricing rule deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        Page<AuditLog> page = auditLogService.getAuditLogs(PageRequest.of(0, 100));
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/reports/csv")
    public void exportCsv(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                          @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                          HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"smartpark_bookings_report.csv\"");
        String csvData = reportService.generateBookingsCsv(startDate, endDate);
        response.getWriter().write(csvData);
        response.getWriter().flush();
    }
}
