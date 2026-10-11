package com.smartpark.controller;

import com.smartpark.dto.*;
import com.smartpark.entity.Booking;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.entity.User;
import com.smartpark.enums.BookingStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.exception.BadRequestException;
import com.smartpark.service.*;
import com.smartpark.util.QrCodeGenerator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/driver")
public class DriverController {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final ParkingZoneService parkingZoneService;
    private final ParkingSlotService parkingSlotService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final QrCodeGenerator qrCodeGenerator;

    public DriverController(UserService userService,
                            VehicleService vehicleService,
                            ParkingZoneService parkingZoneService,
                            ParkingSlotService parkingSlotService,
                            BookingService bookingService,
                            PaymentService paymentService,
                            PricingService pricingService,
                            NotificationService notificationService,
                            QrCodeGenerator qrCodeGenerator) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingZoneService = parkingZoneService;
        this.parkingSlotService = parkingSlotService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            return userService.findByEmail(userDetails.getUsername());
        }
        return null;
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.findByEmail(userDetails.getUsername());
            return notificationService.getUnreadCount(user.getId());
        }
        return 0;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        var vehicles = vehicleService.getVehiclesByUserId(user.getId());
        var bookingsPage = bookingService.getUserBookings(user.getId(), null, PageRequest.of(0, 5, Sort.by("createdAt").descending()));
        var activeBookings = bookingService.getUserBookings(user.getId(), BookingStatus.ACTIVE, PageRequest.of(0, 5, Sort.by("createdAt").descending())).getContent();
        var confirmedBookings = bookingService.getUserBookings(user.getId(), BookingStatus.CONFIRMED, PageRequest.of(0, 5, Sort.by("createdAt").descending())).getContent();

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("recentBookings", bookingsPage.getContent());
        model.addAttribute("activeBookings", activeBookings);
        model.addAttribute("confirmedBookings", confirmedBookings);
        model.addAttribute("totalBookingsCount", bookingsPage.getTotalElements());
        model.addAttribute("totalVehiclesCount", vehicles.size());

        return "driver/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", new ProfileUpdateRequest(user.getName(), user.getPhone()));
        }
        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute("passwordRequest", new PasswordChangeRequest());
        }
        return "driver/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileRequest", result);
            redirectAttributes.addFlashAttribute("profileRequest", request);
            return "redirect:/driver/profile";
        }
        User user = userService.findByEmail(userDetails.getUsername());
        userService.updateProfile(user.getId(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/driver/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("passwordRequest") PasswordChangeRequest request,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordRequest", result);
            redirectAttributes.addFlashAttribute("passwordRequest", request);
            return "redirect:/driver/profile";
        }
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            userService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/driver/profile";
    }

    @GetMapping("/vehicles")
    public String vehicles(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("vehicles", vehicleService.getVehiclesByUserId(user.getId()));
        if (!model.containsAttribute("newVehicle")) {
            model.addAttribute("newVehicle", new VehicleDto());
        }
        return "driver/vehicles";
    }

    @PostMapping("/vehicles/add")
    public String addVehicle(@AuthenticationPrincipal UserDetails userDetails,
                             @Valid @ModelAttribute("newVehicle") VehicleDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.newVehicle", result);
            redirectAttributes.addFlashAttribute("newVehicle", dto);
            return "redirect:/driver/vehicles";
        }
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            vehicleService.addVehicle(user.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("newVehicle", dto);
        }
        return "redirect:/driver/vehicles";
    }

    @PostMapping("/vehicles/edit/{id}")
    public String editVehicle(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable("id") Long vehicleId,
                              @Valid @ModelAttribute("vehicle") VehicleDto dto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid vehicle details");
            return "redirect:/driver/vehicles";
        }
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            vehicleService.updateVehicle(user.getId(), vehicleId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/driver/vehicles";
    }

    @PostMapping("/vehicles/delete/{id}")
    public String deleteVehicle(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable("id") Long vehicleId,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            vehicleService.deleteVehicle(user.getId(), vehicleId);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/driver/vehicles";
    }

    @GetMapping("/search")
    public String searchSlots(@ModelAttribute("searchRequest") SlotSearchRequest request,
                              Model model) {
        if (request.getEntryTime() == null) {
            request.setEntryTime(LocalDateTime.now().plusMinutes(5));
        }
        List<ParkingSlot> availableSlots = parkingSlotService.searchAvailableSlots(request);
        model.addAttribute("zones", parkingZoneService.getActiveZones());
        model.addAttribute("availableSlots", availableSlots);
        model.addAttribute("searchRequest", request);
        return "driver/search";
    }

    @GetMapping("/reserve/{slotId}")
    public String showReserveForm(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable("slotId") Long slotId,
                                  @RequestParam(value = "durationHours", defaultValue = "2") Integer durationHours,
                                  @RequestParam(value = "entryTime", required = false) String entryTimeStr,
                                  Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        ParkingSlot slot = parkingSlotService.getSlotById(slotId);
        var vehicles = vehicleService.getVehiclesByUserId(user.getId());

        LocalDateTime entryTime = LocalDateTime.now().plusMinutes(5);
        if (entryTimeStr != null && !entryTimeStr.isBlank()) {
            try {
                entryTime = LocalDateTime.parse(entryTimeStr);
            } catch (Exception ignored) {}
        }

        BigDecimal estimatedPrice = pricingService.calculatePrice(
                slot.getVehicleType(),
                slot.getZone().getId(),
                entryTime,
                entryTime.plusHours(durationHours)
        );

        BookingRequest bookingRequest = new BookingRequest(slotId, null, entryTime, durationHours);

        model.addAttribute("slot", slot);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("estimatedPrice", estimatedPrice);
        model.addAttribute("bookingRequest", bookingRequest);

        return "driver/reserve";
    }

    @PostMapping("/reserve")
    public String processReservation(@AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @ModelAttribute("bookingRequest") BookingRequest request,
                                     BindingResult result,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide all required reservation fields.");
            return "redirect:/driver/reserve/" + request.getSlotId();
        }

        try {
            User user = userService.findByEmail(userDetails.getUsername());
            Booking booking = bookingService.createBooking(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Parking slot reserved successfully! Booking Code: " + booking.getBookingCode());
            return "redirect:/driver/bookings/" + booking.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/driver/reserve/" + request.getSlotId();
        }
    }

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(value = "status", required = false) BookingStatus status,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "10") int size,
                           Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Page<Booking> bookingsPage = bookingService.getUserBookings(
                user.getId(),
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("bookingsPage", bookingsPage);
        model.addAttribute("currentStatus", status);
        model.addAttribute("statuses", BookingStatus.values());

        return "driver/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetails(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable("id") Long id,
                                 Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Booking booking = bookingService.getBookingById(id);

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != com.smartpark.enums.Role.ROLE_ADMIN) {
            throw new BadRequestException("Unauthorized access to booking details");
        }

        String qrCode = qrCodeGenerator.generateQrCodeBase64(booking.getBookingCode(), 220, 220);

        model.addAttribute("booking", booking);
        model.addAttribute("qrCode", qrCode);

        return "driver/booking-details";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable("id") Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(userDetails.getUsername());
            bookingService.cancelBooking(user.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/driver/bookings/" + id;
    }

    @GetMapping("/bookings/{id}/pay")
    public String paymentPage(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable("id") Long id,
                              Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Booking booking = bookingService.getBookingById(id);

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized access to payment");
        }

        PaymentProcessRequest paymentRequest = new PaymentProcessRequest(booking.getId(), com.smartpark.enums.PaymentMode.ONLINE, booking.getAmount());

        model.addAttribute("booking", booking);
        model.addAttribute("paymentRequest", paymentRequest);

        return "driver/payment";
    }

    @PostMapping("/bookings/{id}/pay")
    public String processPayment(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable("id") Long id,
                                 @ModelAttribute("paymentRequest") PaymentProcessRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            request.setBookingId(id);
            paymentService.processOnlinePayment(request);
            if (request.isSimulateFailure()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Simulated payment failed. Please try again.");
                return "redirect:/driver/bookings/" + id + "/pay";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully!");
            return "redirect:/driver/bookings/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/driver/bookings/" + id + "/pay";
        }
    }

    @GetMapping("/bookings/{id}/receipt")
    public String receipt(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable("id") Long id,
                          Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        Booking booking = bookingService.getBookingById(id);

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != com.smartpark.enums.Role.ROLE_ADMIN) {
            throw new BadRequestException("Unauthorized access to receipt");
        }

        String qrCode = qrCodeGenerator.generateQrCodeBase64(booking.getBookingCode(), 160, 160);
        model.addAttribute("booking", booking);
        model.addAttribute("qrCode", qrCode);

        return "driver/receipt";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "15") int size,
                                Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        var notifs = notificationService.getNotifications(user.getId(), PageRequest.of(page, size));
        model.addAttribute("notificationsPage", notifs);
        return "driver/notifications";
    }

    @PostMapping("/notifications/mark-read/{id}")
    public String markNotificationRead(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable("id") Long id) {
        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.markAsRead(id, user.getId());
        return "redirect:/driver/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        notificationService.markAllAsRead(user.getId());
        return "redirect:/driver/notifications";
    }
}
