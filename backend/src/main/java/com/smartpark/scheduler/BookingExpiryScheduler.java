package com.smartpark.scheduler;

import com.smartpark.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingService bookingService;

    public BookingExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Runs every 60 seconds to release un-checked-in reservations past 15-minute cutoff window.
     */
    @Scheduled(fixedRate = 60000)
    public void sweepExpiredBookings() {
        try {
            int expiredCount = bookingService.expireOverdueBookings();
            if (expiredCount > 0) {
                log.info("BookingExpiryScheduler swept and released {} expired reservations.", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error in BookingExpiryScheduler sweep", e);
        }
    }
}
