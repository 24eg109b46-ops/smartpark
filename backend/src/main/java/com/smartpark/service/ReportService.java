package com.smartpark.service;

import com.smartpark.dto.DashboardStatsDto;

import java.time.LocalDateTime;

public interface ReportService {

    DashboardStatsDto getDashboardStats();

    String generateBookingsCsv(LocalDateTime start, LocalDateTime end);
}
