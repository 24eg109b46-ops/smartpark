package com.smartpark.dto;

import com.smartpark.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentProcessRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment mode is required")
    private PaymentMode mode = PaymentMode.ONLINE;

    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private boolean simulateFailure = false;
    private BigDecimal amount;

    public PaymentProcessRequest() {
    }

    public PaymentProcessRequest(Long bookingId, PaymentMode mode, BigDecimal amount) {
        this.bookingId = bookingId;
        this.mode = mode;
        this.amount = amount;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public PaymentMode getMode() {
        return mode;
    }

    public void setMode(PaymentMode mode) {
        this.mode = mode;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public String getCardCvv() {
        return cardCvv;
    }

    public void setCardCvv(String cardCvv) {
        this.cardCvv = cardCvv;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
