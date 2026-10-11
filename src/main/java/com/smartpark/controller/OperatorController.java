package com.smartpark.controller;

import com.smartpark.dto.BulkSlotCreateRequest;
import com.smartpark.dto.SlotDto;
import com.smartpark.dto.ZoneDto;
import com.smartpark.entity.Booking;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.entity.ParkingZone;
import com.smartpark.entity.User;
import com.smartpark.enums.BookingStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.service.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/operator")
public class OperatorController {

    private final UserService userService;
    private final ParkingZoneService parkingZoneService;
    private final ParkingSlotService parkingSlotService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    public OperatorController(UserService userService,
                              ParkingZoneService parkingZoneService,
                              ParkingSlotService parkingSlotService,
                              BookingService bookingService,
                              PaymentService paymentService) {
        this.userService = userService;
        this.parkingZoneService = parkingZoneService;
        this.parkingSlotService = parkingSlotService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
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
        model.addAttribute("totalSlots", parkingSlotService.countTotalSlots());
        model.addAttribute("availableSlots", parkingSlotService.countSlotsByStatus(SlotStatus.AVAILABLE));
        model.addAttribute("occupiedSlots", parkingSlotService.countSlotsByStatus(SlotStatus.OCCUPIED));
        model.addAttribute("reservedSlots", parkingSlotService.countSlotsByStatus(SlotStatus.RESERVED));
        model.addAttribute("maintenanceSlots", parkingSlotService.countSlotsByStatus(SlotStatus.MAINTENANCE));

        model.addAttribute("todayRevenue", paymentService.getTodayPaidRevenue());
        model.addAttribute("recentBookings", bookingService.getRecentBookings());
        model.addAttribute("zones", parkingZoneService.getAllZones());

        return "operator/dashboard";
    }

    @GetMapping("/zones")
    public String zones(Model model) {
        model.addAttribute("zones", parkingZoneService.getAllZones());
        if (!model.containsAttribute("zoneDto")) {
            model.addAttribute("zoneDto", new ZoneDto());
        }
        return "operator/zones";
    }

    @PostMapping("/zones")
    public String createZone(@Valid @ModelAttribute("zoneDto") ZoneDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.zoneDto", result);
            redirectAttributes.addFlashAttribute("zoneDto", dto);
            return "redirect:/operator/zones";
        }
        try {
            parkingZoneService.createZone(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Parking zone created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/zones";
    }

    @PostMapping("/zones/edit/{id}")
    public String updateZone(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("zoneDto") ZoneDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid zone details");
            return "redirect:/operator/zones";
        }
        try {
            parkingZoneService.updateZone(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Zone updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/zones";
    }

    @PostMapping("/zones/delete/{id}")
    public String deleteZone(@PathVariable("id") Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            parkingZoneService.deleteZone(id);
            redirectAttributes.addFlashAttribute("successMessage", "Zone deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete zone: " + e.getMessage());
        }
        return "redirect:/operator/zones";
    }

    @GetMapping("/slots")
    public String slots(@RequestParam(value = "zoneId", required = false) Long zoneId,
                        Model model) {
        List<ParkingZone> zones = parkingZoneService.getAllZones();
        List<ParkingSlot> slots = (zoneId != null) ?
                parkingSlotService.getSlotsByZone(zoneId) :
                parkingSlotService.getAllSlots();

        model.addAttribute("zones", zones);
        model.addAttribute("slots", slots);
        model.addAttribute("selectedZoneId", zoneId);

        if (!model.containsAttribute("slotDto")) {
            model.addAttribute("slotDto", new SlotDto());
        }
        if (!model.containsAttribute("bulkRequest")) {
            model.addAttribute("bulkRequest", new BulkSlotCreateRequest());
        }

        return "operator/slots";
    }

    @PostMapping("/slots")
    public String createSlot(@Valid @ModelAttribute("slotDto") SlotDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide valid slot parameters.");
            return "redirect:/operator/slots";
        }
        try {
            parkingSlotService.createSlot(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Parking slot added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/slots?zoneId=" + dto.getZoneId();
    }

    @PostMapping("/slots/bulk")
    public String bulkCreateSlots(@Valid @ModelAttribute("bulkRequest") BulkSlotCreateRequest request,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid bulk creation parameters.");
            return "redirect:/operator/slots";
        }
        try {
            int count = parkingSlotService.bulkCreateSlots(request);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully created " + count + " new slots!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/slots?zoneId=" + request.getZoneId();
    }

    @PostMapping("/slots/{id}/status")
    public String updateSlotStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") SlotStatus status,
                                   @RequestParam(value = "redirectZoneId", required = false) Long redirectZoneId,
                                   RedirectAttributes redirectAttributes) {
        try {
            parkingSlotService.updateSlotStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Slot status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return (redirectZoneId != null) ? "redirect:/operator/slots?zoneId=" + redirectZoneId : "redirect:/operator/slots";
    }

    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@PathVariable("id") Long id,
                             @RequestParam(value = "redirectZoneId", required = false) Long redirectZoneId,
                             RedirectAttributes redirectAttributes) {
        try {
            parkingSlotService.deleteSlot(id);
            redirectAttributes.addFlashAttribute("successMessage", "Slot deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete slot: " + e.getMessage());
        }
        return (redirectZoneId != null) ? "redirect:/operator/slots?zoneId=" + redirectZoneId : "redirect:/operator/slots";
    }

    @GetMapping("/occupancy")
    public String occupancyView(@RequestParam(value = "zoneId", required = false) Long zoneId,
                                Model model) {
        List<ParkingZone> zones = parkingZoneService.getAllZones();
        Long activeZoneId = zoneId != null ? zoneId : (zones.isEmpty() ? null : zones.get(0).getId());

        List<ParkingSlot> slots = (activeZoneId != null) ?
                parkingSlotService.getSlotsByZone(activeZoneId) : List.of();

        model.addAttribute("zones", zones);
        model.addAttribute("selectedZoneId", activeZoneId);
        model.addAttribute("slots", slots);

        return "operator/occupancy";
    }

    @GetMapping("/bookings")
    public String bookings(@RequestParam(value = "status", required = false) BookingStatus status,
                           @RequestParam(value = "query", required = false) String query,
                           @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                           @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "15") int size,
                           Model model) {
        Page<Booking> bookingsPage = bookingService.searchBookings(
                status,
                query,
                startDate,
                endDate,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("currentStatus", status);
        model.addAttribute("query", query);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("statuses", BookingStatus.values());

        return "operator/bookings";
    }

    @PostMapping("/bookings/check-in/{id}")
    public String checkIn(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.checkIn(id);
            redirectAttributes.addFlashAttribute("successMessage", "Check-in recorded for " + booking.getBookingCode() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/bookings";
    }

    @PostMapping("/bookings/check-out/{id}")
    public String checkOut(@PathVariable("id") Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.checkOut(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-out recorded for " + booking.getBookingCode() + "! Total charges: $" + booking.getAmount());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/bookings";
    }

    @PostMapping("/bookings/cash-payment/{id}")
    public String recordCashPayment(@PathVariable("id") Long id,
                                    @RequestParam("amount") BigDecimal amount,
                                    RedirectAttributes redirectAttributes) {
        try {
            paymentService.recordCashPayment(id, amount);
            redirectAttributes.addFlashAttribute("successMessage", "Cash payment of $" + amount + " successfully recorded!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operator/bookings";
    }
}
