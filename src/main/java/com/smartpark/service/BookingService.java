package com.smartpark.service;

import com.smartpark.dto.BookingRequest;
import com.smartpark.entity.Booking;
import com.smartpark.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    Booking createBooking(Long userId, BookingRequest request);

    Booking cancelBooking(Long userId, Long bookingId);

    Booking getBookingById(Long id);

    Booking getBookingByCode(String bookingCode);

    Page<Booking> getUserBookings(Long userId, BookingStatus status, Pageable pageable);

    Page<Booking> searchBookings(BookingStatus status, String query, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Booking checkIn(Long bookingId);

    Booking checkOut(Long bookingId);

    int expireOverdueBookings();

    long countTotalBookings();

    long countBookingsByStatus(BookingStatus status);

    List<Booking> getRecentBookings();
}
