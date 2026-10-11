package com.smartpark.service.impl;

import com.smartpark.dto.DashboardStatsDto;
import com.smartpark.entity.Booking;
import com.smartpark.entity.ParkingZone;
import com.smartpark.enums.BookingStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.ParkingZoneRepository;
import com.smartpark.repository.PaymentRepository;
import com.smartpark.repository.UserRepository;
import com.smartpark.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(ParkingSlotRepository parkingSlotRepository,
                             ParkingZoneRepository parkingZoneRepository,
                             BookingRepository bookingRepository,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingZoneRepository = parkingZoneRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalSlots(parkingSlotRepository.count());
        stats.setAvailableSlots(parkingSlotRepository.countByStatus(SlotStatus.AVAILABLE));
        stats.setOccupiedSlots(parkingSlotRepository.countByStatus(SlotStatus.OCCUPIED));
        stats.setReservedSlots(parkingSlotRepository.countByStatus(SlotStatus.RESERVED));
        stats.setMaintenanceSlots(parkingSlotRepository.countByStatus(SlotStatus.MAINTENANCE));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        stats.setTodayBookings(bookingRepository.countByCreatedAtBetween(startOfDay, endOfDay));

        BigDecimal todayRev = paymentRepository.sumPaidRevenueBetween(startOfDay, endOfDay);
        stats.setTodayRevenue(todayRev != null ? todayRev : BigDecimal.ZERO);

        BigDecimal totalRev = paymentRepository.sumTotalPaidRevenue();
        stats.setTotalRevenue(totalRev != null ? totalRev : BigDecimal.ZERO);

        stats.setTotalUsers(userRepository.count());
        stats.setTotalDrivers(userRepository.countByRole(com.smartpark.enums.Role.ROLE_DRIVER));
        stats.setTotalOperators(userRepository.countByRole(com.smartpark.enums.Role.ROLE_OPERATOR));

        // Occupancy by Zone
        List<ParkingZone> zones = parkingZoneRepository.findAll();
        Map<String, Long> zoneOccupancy = new LinkedHashMap<>();
        for (ParkingZone zone : zones) {
            long occupied = parkingSlotRepository.countByZoneIdAndStatus(zone.getId(), SlotStatus.OCCUPIED);
            zoneOccupancy.put(zone.getName(), occupied);
        }
        stats.setOccupancyByZone(zoneOccupancy);

        // Bookings by Status
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            long count = bookingRepository.countByStatus(status);
            statusMap.put(status.name(), count);
        }
        stats.setBookingsByStatus(statusMap);

        // Revenue last 7 days
        Map<String, BigDecimal> last7Days = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MMM dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(LocalTime.MAX);
            BigDecimal dayRev = paymentRepository.sumPaidRevenueBetween(dayStart, dayEnd);
            last7Days.put(d.format(dayFormatter), dayRev != null ? dayRev : BigDecimal.ZERO);
        }
        stats.setRevenueLast7Days(last7Days);

        return stats;
    }

    @Override
    public String generateBookingsCsv(LocalDateTime start, LocalDateTime end) {
        List<Booking> bookings = bookingRepository.findForReport(start, end);
        StringBuilder sb = new StringBuilder();
        sb.append("Booking Code,User Name,User Email,Vehicle Plate,Vehicle Type,Zone,Slot Number,Entry Time,Exit Time,Status,Amount,Payment Status\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Booking b : bookings) {
            String paymentStatus = (b.getPayment() != null) ? b.getPayment().getStatus().name() : "N/A";
            sb.append(escapeCsv(b.getBookingCode())).append(",")
              .append(escapeCsv(b.getUser().getName())).append(",")
              .append(escapeCsv(b.getUser().getEmail())).append(",")
              .append(escapeCsv(b.getVehicle().getVehicleNumber())).append(",")
              .append(escapeCsv(b.getVehicle().getVehicleType().name())).append(",")
              .append(escapeCsv(b.getSlot().getZone().getName())).append(",")
              .append(escapeCsv(b.getSlot().getSlotNumber())).append(",")
              .append(b.getEntryTime() != null ? b.getEntryTime().format(dtf) : "").append(",")
              .append(b.getExitTime() != null ? b.getExitTime().format(dtf) : "").append(",")
              .append(b.getStatus().name()).append(",")
              .append(b.getAmount() != null ? b.getAmount() : "0.00").append(",")
              .append(paymentStatus).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
