package com.smartpark.repository;

import com.smartpark.entity.Booking;
import com.smartpark.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusIn(Collection<BookingStatus> statuses);

    Page<Booking> findByStatusIn(Collection<BookingStatus> statuses, Pageable pageable);

    long countByStatus(BookingStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId " +
           "AND b.status IN (:statuses) " +
           "AND b.entryTime < :requestedExit " +
           "AND b.exitTime > :requestedEntry")
    List<Booking> findOverlappingBookings(
            @Param("slotId") Long slotId,
            @Param("requestedEntry") LocalDateTime requestedEntry,
            @Param("requestedExit") LocalDateTime requestedExit,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("SELECT b FROM Booking b WHERE b.status IN (:statuses) AND b.entryTime < :cutoffTime")
    List<Booking> findUncheckedInBookingsPastCutoff(
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:query IS NULL OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.vehicle.vehicleNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.slot.slotNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:startDate IS NULL OR b.entryTime >= :startDate) AND " +
           "(:endDate IS NULL OR b.entryTime <= :endDate)")
    Page<Booking> searchBookings(
            @Param("status") BookingStatus status,
            @Param("query") String query,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT b FROM Booking b WHERE " +
           "(:startDate IS NULL OR b.entryTime >= :startDate) AND " +
           "(:endDate IS NULL OR b.entryTime <= :endDate) " +
           "ORDER BY b.entryTime DESC")
    List<Booking> findForReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Booking> findTop10ByOrderByCreatedAtDesc();
}
