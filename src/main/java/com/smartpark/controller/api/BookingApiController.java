package com.smartpark.controller.api;

import com.smartpark.dto.BookingRequest;
import com.smartpark.dto.PaymentProcessRequest;
import com.smartpark.entity.Booking;
import com.smartpark.enums.BookingStatus;
import com.smartpark.service.BookingService;
import com.smartpark.service.PaymentService;
import com.smartpark.util.QrCodeGenerator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingApiController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final QrCodeGenerator qrCodeGenerator;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public BookingApiController(BookingService bookingService,
                                PaymentService paymentService,
                                QrCodeGenerator qrCodeGenerator) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam("userId") Long userId,
                                           @Valid @RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(userId, request);
            return ResponseEntity.ok(toMap(booking, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable("id") Long id) {
        try {
            Booking booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(toMap(booking, true));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserBookings(@PathVariable("userId") Long userId) {
        Page<Booking> page = bookingService.getUserBookings(userId, null, PageRequest.of(0, 50, Sort.by("createdAt").descending()));
        List<Map<String, Object>> result = page.getContent().stream()
                .map(b -> toMap(b, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllBookings(
            @RequestParam(value = "status", required = false) BookingStatus status,
            @RequestParam(value = "query", required = false) String query) {

        Page<Booking> page = bookingService.searchBookings(status, query, null, null, PageRequest.of(0, 100, Sort.by("createdAt").descending()));
        List<Map<String, Object>> result = page.getContent().stream()
                .map(b -> toMap(b, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkIn(@PathVariable("id") Long id) {
        try {
            Booking booking = bookingService.checkIn(id);
            return ResponseEntity.ok(Map.of("message", "Checked in successfully", "booking", toMap(booking, false)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(@PathVariable("id") Long id) {
        try {
            Booking booking = bookingService.checkOut(id);
            return ResponseEntity.ok(Map.of("message", "Checked out successfully", "booking", toMap(booking, false)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        try {
            Booking booking = bookingService.cancelBooking(userId, id);
            return ResponseEntity.ok(Map.of("message", "Cancelled successfully", "booking", toMap(booking, false)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payOnline(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        try {
            PaymentProcessRequest request = new PaymentProcessRequest();
            request.setBookingId(id);
            request.setMode(com.smartpark.enums.PaymentMode.ONLINE);
            request.setSimulateFailure(Boolean.TRUE.equals(body.get("simulateFailure")));
            paymentService.processOnlinePayment(request);
            Booking updated = bookingService.getBookingById(id);
            return ResponseEntity.ok(Map.of("message", "Payment processed successfully", "booking", toMap(updated, false)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cash")
    public ResponseEntity<?> payCash(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        try {
            BigDecimal amount = body.get("amount") != null ? new BigDecimal(body.get("amount").toString()) : null;
            paymentService.recordCashPayment(id, amount);
            Booking updated = bookingService.getBookingById(id);
            return ResponseEntity.ok(Map.of("message", "Cash payment recorded", "booking", toMap(updated, false)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(Booking b, boolean includeQr) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", b.getId());
        map.put("bookingCode", b.getBookingCode());
        map.put("userName", b.getUser().getName());
        map.put("userEmail", b.getUser().getEmail());
        map.put("vehicleNumber", b.getVehicle().getVehicleNumber());
        map.put("vehicleType", b.getVehicle().getVehicleType().name());
        map.put("slotNumber", b.getSlot().getSlotNumber());
        map.put("zoneName", b.getSlot.getZone().getName());
        map.put("zoneLocation", b.getSlot().getZone().getLocation());
        map.put("entryTime", b.getEntryTime() != null ? b.getEntryTime().format(dtf) : null);
        map.put("exitTime", b.getExitTime() != null ? b.getExitTime().format(dtf) : null);
        map.put("status", b.getStatus().name());
        map.put("amount", b.getAmount());

        if (b.getPayment() != null) {
            map.put("paymentStatus", b.getPayment().getStatus().name());
            map.put("paymentMode", b.getPayment().getMode().name());
            map.put("transactionRef", b.getPayment().getTransactionRef());
        } else {
            map.put("paymentStatus", "N/A");
        }

        if (includeQr) {
            map.put("qrCode", qrCodeGenerator.generateQrCodeBase64(b.getBookingCode(), 220, 220));
        }

        return map;
    }
}
