package com.smartpark.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.dto.*;
import com.smartpark.entity.AuditLog;
import com.smartpark.entity.User;
import com.smartpark.enums.Role;
import com.smartpark.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ParkingZoneService parkingZoneService;
    private final PricingService pricingService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(UserService userService,
                           ParkingZoneService parkingZoneService,
                           PricingService pricingService,
                           ReportService reportService,
                           AuditLogService auditLogService) {
        this.userService = userService;
        this.parkingZoneService = parkingZoneService;
        this.pricingService = pricingService;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return userService.findByEmail(userDetails.getUsername());
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDto stats = reportService.getDashboardStats();
        model.addAttribute("stats", stats);

        try {
            // Chart.js JSON payloads
            model.addAttribute("occupancyLabelsJson", objectMapper.writeValueAsString(stats.getOccupancyByZone().keySet()));
            model.addAttribute("occupancyValuesJson", objectMapper.writeValueAsString(stats.getOccupancyByZone().values()));

            model.addAttribute("statusLabelsJson", objectMapper.writeValueAsString(stats.getBookingsByStatus().keySet()));
            model.addAttribute("statusValuesJson", objectMapper.writeValueAsString(stats.getBookingsByStatus().values()));

            model.addAttribute("revenueLabelsJson", objectMapper.writeValueAsString(stats.getRevenueLast7Days().keySet()));
            model.addAttribute("revenueValuesJson", objectMapper.writeValueAsString(stats.getRevenueLast7Days().values()));
        } catch (Exception e) {
            model.addAttribute("occupancyLabelsJson", "[]");
            model.addAttribute("occupancyValuesJson", "[]");
            model.addAttribute("statusLabelsJson", "[]");
            model.addAttribute("statusValuesJson", "[]");
            model.addAttribute("revenueLabelsJson", "[]");
            model.addAttribute("revenueValuesJson", "[]");
        }

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "role", required = false) Role role,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "15") int size,
                        Model model) {
        Page<User> usersPage = (role != null) ?
                userService.getUsersByRole(role, PageRequest.of(page, size, Sort.by("createdAt").descending())) :
                userService.getAllUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("selectedRole", role);
        model.addAttribute("roles", Role.values());

        if (!model.containsAttribute("newOperator")) {
            model.addAttribute("newOperator", new RegisterRequest());
        }

        return "admin/users";
    }

    @PostMapping("/users/create-operator")
    public String createOperator(@Valid @ModelAttribute("newOperator") RegisterRequest request,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.newOperator", "Passwords do not match");
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newOperator", result);
            redirectAttributes.addFlashAttribute("newOperator", request);
            return "redirect:/admin/users";
        }
        try {
            userService.createOperator(request);
            redirectAttributes.addFlashAttribute("successMessage", "Parking Operator created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated to " + user.getStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/pricing")
    public String pricing(Model model) {
        model.addAttribute("rules", pricingService.getAllRules());
        model.addAttribute("zones", parkingZoneService.getAllZones());
        if (!model.containsAttribute("pricingDto")) {
            model.addAttribute("pricingDto", new PricingRuleDto());
        }
        return "admin/pricing";
    }

    @PostMapping("/pricing")
    public String createPricingRule(@Valid @ModelAttribute("pricingDto") PricingRuleDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid pricing rule fields.");
            return "redirect:/admin/pricing";
        }
        try {
            pricingService.createRule(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/pricing";
    }

    @PostMapping("/pricing/edit/{id}")
    public String updatePricingRule(@PathVariable("id") Long id,
                                    @Valid @ModelAttribute("pricingDto") PricingRuleDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid pricing rule fields.");
            return "redirect:/admin/pricing";
        }
        try {
            pricingService.updateRule(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/pricing";
    }

    @PostMapping("/pricing/delete/{id}")
    public String deletePricingRule(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            pricingService.deleteRule(id);
            redirectAttributes.addFlashAttribute("successMessage", "Pricing rule deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/pricing";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                          @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                          Model model) {
        DashboardStatsDto stats = reportService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/reports";
    }

    @GetMapping("/reports/export-csv")
    public void exportBookingsCsv(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                  @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                  HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"smartpark_bookings_report.csv\"");
        String csvData = reportService.generateBookingsCsv(startDate, endDate);
        response.getWriter().write(csvData);
        response.getWriter().flush();
    }

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(value = "action", required = false) String action,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            Model model) {
        Page<AuditLog> logs = (action != null && !action.isBlank()) ?
                auditLogService.getAuditLogsByAction(action.trim(), PageRequest.of(page, size)) :
                auditLogService.getAuditLogs(PageRequest.of(page, size));

        model.addAttribute("logsPage", logs);
        model.addAttribute("selectedAction", action);
        return "admin/audit-logs";
    }
}
