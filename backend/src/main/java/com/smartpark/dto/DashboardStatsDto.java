package com.smartpark.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DashboardStatsDto {

    private long totalSlots;
    private long availableSlots;
    private long occupiedSlots;
    private long reservedSlots;
    private long maintenanceSlots;

    private long todayBookings;
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private long totalUsers;
    private long totalDrivers;
    private long totalOperators;

    // Chart Data
    private Map<String, Long> occupancyByZone = new HashMap<>();
    private Map<String, Long> bookingsByStatus = new HashMap<>();
    private Map<String, BigDecimal> revenueLast7Days = new HashMap<>();

    public DashboardStatsDto() {
    }

    public long getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(long totalSlots) {
        this.totalSlots = totalSlots;
    }

    public long getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(long availableSlots) {
        this.availableSlots = availableSlots;
    }

    public long getOccupiedSlots() {
        return occupiedSlots;
    }

    public void setOccupiedSlots(long occupiedSlots) {
        this.occupiedSlots = occupiedSlots;
    }

    public long getReservedSlots() {
        return reservedSlots;
    }

    public void setReservedSlots(long reservedSlots) {
        this.reservedSlots = reservedSlots;
    }

    public long getMaintenanceSlots() {
        return maintenanceSlots;
    }

    public void setMaintenanceSlots(long maintenanceSlots) {
        this.maintenanceSlots = maintenanceSlots;
    }

    public long getTodayBookings() {
        return todayBookings;
    }

    public void setTodayBookings(long todayBookings) {
        this.todayBookings = todayBookings;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDrivers() {
        return totalDrivers;
    }

    public void setTotalDrivers(long totalDrivers) {
        this.totalDrivers = totalDrivers;
    }

    public long getTotalOperators() {
        return totalOperators;
    }

    public void setTotalOperators(long totalOperators) {
        this.totalOperators = totalOperators;
    }

    public Map<String, Long> getOccupancyByZone() {
        return occupancyByZone;
    }

    public void setOccupancyByZone(Map<String, Long> occupancyByZone) {
        this.occupancyByZone = occupancyByZone;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public Map<String, BigDecimal> getRevenueLast7Days() {
        return revenueLast7Days;
    }

    public void setRevenueLast7Days(Map<String, BigDecimal> revenueLast7Days) {
        this.revenueLast7Days = revenueLast7Days;
    }
}
