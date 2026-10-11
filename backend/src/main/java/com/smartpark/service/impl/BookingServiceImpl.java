package com.smartpark.service.impl;

import com.smartpark.dto.BookingRequest;
import com.smartpark.entity.*;
import com.smartpark.enums.*;
import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.InvalidBookingStateException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.exception.SlotNotAvailableException;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.PaymentRepository;
import com.smartpark.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ParkingSlotRepository parkingSlotRepository,
                              PaymentRepository paymentRepository,
                              UserService userService,
                              VehicleService vehicleService,
                              PricingService pricingService,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.paymentRepository = paymentRepository;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public Booking createBooking(Long userId, BookingRequest request) {
        User user = userService.findById(userId);
        Vehicle vehicle = vehicleService.getVehicleById(request.getVehicleId());

        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Selected vehicle does not belong to your account");
        }

        // Pessimistic lock on slot to ensure thread-safety and prevent double-booking
        ParkingSlot slot = parkingSlotRepository.findByIdWithPessimisticLock(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: " + request.getSlotId()));

        if (slot.getStatus() == SlotStatus.MAINTENANCE) {
            throw new SlotNotAvailableException("Slot " + slot.getSlotNumber() + " is currently under maintenance");
        }

        if (slot.getVehicleType() != vehicle.getVehicleType()) {
            throw new BadRequestException("Slot " + slot.getSlotNumber() + " is designated for "
                    + slot.getVehicleType() + ", but your vehicle is " + vehicle.getVehicleType());
        }

        LocalDateTime entryTime = request.getEntryTime() != null ? request.getEntryTime() : LocalDateTime.now();
        int durationHours = (request.getDurationHours() != null && request.getDurationHours() > 0) ? request.getDurationHours() : 1;
        LocalDateTime exitTime = entryTime.plusHours(durationHours);

        // Check overlapping active bookings
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(slot.getId(), entryTime, exitTime, activeStatuses);
        if (!overlaps.isEmpty()) {
            throw new SlotNotAvailableException("Parking slot " + slot.getSlotNumber() + " is already reserved for the selected time window.");
        }

        // Generate unique booking code
        String bookingCode = "SP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Calculate estimated amount
        BigDecimal estimatedAmount = pricingService.calculatePrice(
                vehicle.getVehicleType(),
                slot.getZone().getId(),
                entryTime,
                exitTime
        );

        Booking booking = new Booking(
                bookingCode,
                user,
                vehicle,
                slot,
                entryTime,
                durationHours,
                BookingStatus.CONFIRMED,
                estimatedAmount
        );
        booking.setExitTime(exitTime);

        // If booking starts within 30 minutes from now, mark slot as RESERVED
        LocalDateTime now = LocalDateTime.now();
        if (entryTime.isBefore(now.plusMinutes(30))) {
            slot.setStatus(SlotStatus.RESERVED);
            parkingSlotRepository.save(slot);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Create initial pending payment record
        Payment payment = new Payment(savedBooking, estimatedAmount, PaymentMode.ONLINE, "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), PaymentStatus.PENDING);
        savedBooking.setPayment(payment);
        paymentRepository.save(payment);

        // Send confirmation notification
        notificationService.sendNotification(
                user,
                "Booking Confirmed",
                "Your reservation " + bookingCode + " for slot " + slot.getSlotNumber() + " in " + slot.getZone().getName() + " is confirmed.",
                NotificationType.SUCCESS
        );

        auditLogService.log(user.getId(), user.getEmail(), "BOOKING_CREATED",
                "Created booking " + bookingCode + " for slot " + slot.getSlotNumber(), null);

        return savedBooking;
    }

    @Override
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = getBookingById(bookingId);
        User user = userService.findById(userId);

        if (!booking.getUser().getId().equals(userId) && user.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("Unauthorized access to booking cancellation");
        }

        if (booking.getStatus() == BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException("Cannot cancel an active parked booking. Please check-out instead.");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED ||
            booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.EXPIRED) {
            throw new InvalidBookingStateException("Booking is already " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Free slot if it was reserved by this booking
        ParkingSlot slot = booking.getSlot();
        if (slot.getStatus() == SlotStatus.RESERVED) {
            slot.setStatus(SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }

        // Refund payment if paid
        if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.PAID) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(booking.getPayment());
        }

        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getUser(),
                "Booking Cancelled",
                "Your reservation " + booking.getBookingCode() + " has been cancelled.",
                NotificationType.WARNING
        );

        auditLogService.log(user.getId(), user.getEmail(), "BOOKING_CANCELLED",
                "Cancelled booking " + booking.getBookingCode(), null);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getUserBookings(Long userId, BookingStatus status, Pageable pageable) {
        if (status != null) {
            return bookingRepository.findByUserIdAndStatus(userId, status, pageable);
        }
        return bookingRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> searchBookings(BookingStatus status, String query, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return bookingRepository.searchBookings(status, query, start, end, pageable);
    }

    @Override
    public Booking checkIn(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Cannot check-in booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setEntryTime(LocalDateTime.now());

        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.OCCUPIED);
        parkingSlotRepository.save(slot);

        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getUser(),
                "Check-In Confirmed",
                "Vehicle " + booking.getVehicle().getVehicleNumber() + " is now checked into slot " + slot.getSlotNumber() + ".",
                NotificationType.SUCCESS
        );

        auditLogService.log(null, null, "CHECK_IN",
                "Checked in booking " + booking.getBookingCode() + " into slot " + slot.getSlotNumber(), null);

        return saved;
    }

    @Override
    public Booking checkOut(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException("Cannot check-out booking that is not currently ACTIVE. Current status: " + booking.getStatus());
        }

        LocalDateTime actualExitTime = LocalDateTime.now();
        booking.setExitTime(actualExitTime);
        booking.setStatus(BookingStatus.COMPLETED);

        // Compute final amount based on actual duration and active pricing rule
        BigDecimal finalAmount = pricingService.calculatePrice(
                booking.getVehicle().getVehicleType(),
                booking.getSlot().getZone().getId(),
                booking.getEntryTime(),
                actualExitTime
        );
        booking.setAmount(finalAmount);

        // Free the slot
        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        parkingSlotRepository.save(slot);

        // Update payment record amount
        Payment payment = booking.getPayment();
        if (payment != null) {
            payment.setAmount(finalAmount);
            paymentRepository.save(payment);
        } else {
            payment = new Payment(booking, finalAmount, PaymentMode.CASH, "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), PaymentStatus.PENDING);
            booking.setPayment(payment);
            paymentRepository.save(payment);
        }

        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                booking.getUser(),
                "Check-Out Completed",
                "Parking session completed for " + booking.getBookingCode() + ". Total charged: $" + finalAmount,
                NotificationType.INFO
        );

        auditLogService.log(null, null, "CHECK_OUT",
                "Checked out booking " + booking.getBookingCode() + ". Final amount: $" + finalAmount, null);

        return saved;
    }

    @Override
    public int expireOverdueBookings() {
        // Bookings not checked-in within 15 minutes of scheduled entry time
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<BookingStatus> pendingStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        List<Booking> expiredBookings = bookingRepository.findUncheckedInBookingsPastCutoff(pendingStatuses, cutoff);

        int count = 0;
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);

            ParkingSlot slot = booking.getSlot();
            if (slot.getStatus() == SlotStatus.RESERVED) {
                slot.setStatus(SlotStatus.AVAILABLE);
                parkingSlotRepository.save(slot);
            }

            bookingRepository.save(booking);

            notificationService.sendNotification(
                    booking.getUser(),
                    "Reservation Expired",
                    "Your reservation " + booking.getBookingCode() + " has expired due to no-show within 15 minutes of scheduled time.",
                    NotificationType.DANGER
            );

            auditLogService.log(booking.getUser().getId(), booking.getUser().getEmail(), "BOOKING_EXPIRED",
                    "Expired overdue booking " + booking.getBookingCode(), null);

            count++;
        }

        if (count > 0) {
            log.info("Expired {} overdue un-checked-in bookings", count);
        }

        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalBookings() {
        return bookingRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBookingsByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getRecentBookings() {
        return bookingRepository.findTop10ByOrderByCreatedAtDesc();
    }
}
