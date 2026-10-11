package com.smartpark.service;

import com.smartpark.dto.PaymentProcessRequest;
import com.smartpark.entity.Payment;

import java.math.BigDecimal;

public interface PaymentService {

    Payment processOnlinePayment(PaymentProcessRequest request);

    Payment recordCashPayment(Long bookingId, BigDecimal amount);

    Payment getPaymentByBookingId(Long bookingId);

    BigDecimal getTotalPaidRevenue();

    BigDecimal getTodayPaidRevenue();
}
