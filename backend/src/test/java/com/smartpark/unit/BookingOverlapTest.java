package com.smartpark.unit;

import com.smartpark.dto.BookingRequest;
import com.smartpark.entity.*;
import com.smartpark.enums.*;
import com.smartpark.exception.SlotNotAvailableException;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.PaymentRepository;
import com.smartpark.service.*;
import com.smartpark.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOverlapTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ParkingSlotRepository parkingSlotRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserService userService;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private PricingService pricingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Vehicle testVehicle;
    private ParkingSlot testSlot;
    private ParkingZone testZone;

    @BeforeEach
    void setUp() {
        testUser = new User("Alice", "alice@test.com", "1234567890", "pass", Role.ROLE_DRIVER, UserStatus.ACTIVE);
        testUser.setId(10L);

        testVehicle = new Vehicle(testUser, "TEST-PLATE-1", VehicleType.CAR, "Ford", "Focus");
        testVehicle.setId(20L);

        testZone = new ParkingZone("Zone A", "North", "1", 10, ZoneStatus.ACTIVE);
        testZone.setId(30L);

        testSlot = new ParkingSlot(testZone, "A-01", VehicleType.CAR, SlotStatus.AVAILABLE);
        testSlot.setId(40L);
    }

    @Test
    @DisplayName("Should throw SlotNotAvailableException when another booking overlaps the requested time window")
    void testOverlapThrowsException() {
        when(userService.findById(10L)).thenReturn(testUser);
        when(vehicleService.getVehicleById(20L)).thenReturn(testVehicle);
        when(parkingSlotRepository.findByIdWithPessimisticLock(40L)).thenReturn(Optional.of(testSlot));

        LocalDateTime requestedEntry = LocalDateTime.now().plusHours(1);
        LocalDateTime requestedExit = requestedEntry.plusHours(2);

        Booking existingBooking = new Booking("SP-EXISTING", testUser, testVehicle, testSlot, requestedEntry.minusMinutes(30), 2, BookingStatus.CONFIRMED, BigDecimal.TEN);
        existingBooking.setExitTime(requestedEntry.plusHours(1));

        // Mock overlap query finding an overlapping booking
        when(bookingRepository.findOverlappingBookings(eq(40L), eq(requestedEntry), eq(requestedExit), any()))
                .thenReturn(List.of(existingBooking));

        BookingRequest request = new BookingRequest(40L, 20L, requestedEntry, 2);

        assertThrows(SlotNotAvailableException.class, () -> {
            bookingService.createBooking(10L, request);
        });
    }

    @Test
    @DisplayName("Should successfully create booking when no overlapping bookings exist")
    void testNonOverlappingSucceeds() {
        when(userService.findById(10L)).thenReturn(testUser);
        when(vehicleService.getVehicleById(20L)).thenReturn(testVehicle);
        when(parkingSlotRepository.findByIdWithPessimisticLock(40L)).thenReturn(Optional.of(testSlot));

        LocalDateTime requestedEntry = LocalDateTime.now().plusHours(5);
        LocalDateTime requestedExit = requestedEntry.plusHours(2);

        // No overlaps found
        when(bookingRepository.findOverlappingBookings(eq(40L), eq(requestedEntry), eq(requestedExit), any()))
                .thenReturn(List.of());

        when(pricingService.calculatePrice(any(), any(), any(), any())).thenReturn(new BigDecimal("40.00"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingRequest request = new BookingRequest(40L, 20L, requestedEntry, 2);
        Booking created = bookingService.createBooking(10L, request);

        assertNotNull(created);
        assertEquals(BookingStatus.CONFIRMED, created.getStatus());
        assertEquals(new BigDecimal("40.00"), created.getAmount());
    }
}
