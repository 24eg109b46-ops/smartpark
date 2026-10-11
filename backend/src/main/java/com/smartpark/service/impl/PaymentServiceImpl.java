package com.smartpark.service.impl;

import com.smartpark.dto.PaymentProcessRequest;
import com.smartpark.entity.Booking;
import com.smartpark.entity.Payment;
import com.smartpark.enums.NotificationType;
import com.smartpark.enums.PaymentMode;
import com.smartpark.enums.PaymentStatus;
import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.PaymentRepository;
import com.smartpark.service.AuditLogService;
import com.smartpark.service.NotificationService;
import com.smartpark.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public Payment processOnlinePayment(PaymentProcessRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This booking has already been paid for.");
        }

        payment.setMode(PaymentMode.ONLINE);
        payment.setTransactionRef("ONL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());

        if (request.isSimulateFailure()) {
            payment.setStatus(PaymentStatus.FAILED);
            Payment saved = paymentRepository.save(payment);
            notificationService.sendNotification(
                    booking.getUser(),
                    "Payment Failed",
                    "Simulated payment failed for booking " + booking.getBookingCode(),
                    NotificationType.DANGER
            );
            return saved;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        notificationService.sendNotification(
                booking.getUser(),
                "Payment Received",
                "Payment of $" + payment.getAmount() + " received successfully for booking " + booking.getBookingCode(),
                NotificationType.SUCCESS
        );

        auditLogService.log(booking.getUser().getId(), booking.getUser().getEmail(), "PAYMENT_ONLINE",
                "Processed online payment of $" + payment.getAmount() + " for booking " + booking.getBookingCode(), null);

        return saved;
    }

    @Override
    public Payment recordCashPayment(Long bookingId, BigDecimal amount) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setBooking(booking);
        }

        payment.setAmount(amount != null ? amount : (booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO));
        payment.setMode(PaymentMode.CASH);
        payment.setTransactionRef("CSH-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        notificationService.sendNotification(
                booking.getUser(),
                "Cash Payment Recorded",
                "Cash payment of $" + payment.getAmount() + " has been recorded by parking operator for booking " + booking.getBookingCode(),
                NotificationType.SUCCESS
        );

        auditLogService.log(null, null, "PAYMENT_CASH",
                "Operator recorded cash payment of $" + payment.getAmount() + " for booking " + booking.getBookingCode(), null);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking ID: " + bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidRevenue() {
        BigDecimal total = paymentRepository.sumTotalPaidRevenue();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTodayPaidRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal today = paymentRepository.sumPaidRevenueBetween(startOfDay, endOfDay);
        return today != null ? today : BigDecimal.ZERO;
    }
}
