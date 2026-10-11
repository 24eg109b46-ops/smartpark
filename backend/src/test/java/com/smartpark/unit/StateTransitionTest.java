package com.smartpark.unit;

import com.smartpark.entity.*;
import com.smartpark.enums.*;
import com.smartpark.exception.InvalidBookingStateException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateTransitionTest {

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

    private User user;
    private Vehicle vehicle;
    private ParkingSlot slot;
    private ParkingZone zone;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = new User("Bob", "bob@test.com", "555-1234", "pass", Role.ROLE_DRIVER, UserStatus.ACTIVE);
        user.setId(1L);

        vehicle = new Vehicle(user, "BOB-123", VehicleType.CAR, "Toyota", "Corolla");
        vehicle.setId(2L);

        zone = new ParkingZone("Zone A", "T1", "1", 10, ZoneStatus.ACTIVE);
        zone.setId(3L);

        slot = new ParkingSlot(zone, "A-01", VehicleType.CAR, SlotStatus.RESERVED);
        slot.setId(4L);

        booking = new Booking("SP-BOB01", user, vehicle, slot, LocalDateTime.now(), 2, BookingStatus.CONFIRMED, new BigDecimal("50.00"));
        booking.setId(5L);
    }

    @Test
    @DisplayName("Check-In: Transition CONFIRMED -> ACTIVE and Slot -> OCCUPIED")
    void testCheckInSuccess() {
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking checkedIn = bookingService.checkIn(5L);

        assertEquals(BookingStatus.ACTIVE, checkedIn.getStatus());
        assertEquals(SlotStatus.OCCUPIED, slot.getStatus());
        verify(parkingSlotRepository).save(slot);
    }

    @Test
    @DisplayName("Check-In: Throws InvalidBookingStateException if booking is already COMPLETED")
    void testCheckInInvalidState() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.checkIn(5L));
    }

    @Test
    @DisplayName("Check-Out: Transition ACTIVE -> COMPLETED and Slot -> AVAILABLE, recalculating final price")
    void testCheckOutSuccess() {
        booking.setStatus(BookingStatus.ACTIVE);
        slot.setStatus(SlotStatus.OCCUPIED);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(pricingService.calculatePrice(any(), any(), any(), any())).thenReturn(new BigDecimal("75.00"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking checkedOut = bookingService.checkOut(5L);

        assertEquals(BookingStatus.COMPLETED, checkedOut.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());
        assertEquals(new BigDecimal("75.00"), checkedOut.getAmount());
        assertNotNull(checkedOut.getExitTime());
        verify(parkingSlotRepository).save(slot);
    }

    @Test
    @DisplayName("Check-Out: Throws InvalidBookingStateException if booking is not ACTIVE")
    void testCheckOutNotActiveThrows() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.checkOut(5L));
    }

    @Test
    @DisplayName("Cancellation: Prohibited when booking is already ACTIVE (in session)")
    void testCancellationProhibitedWhenActive() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userService.findById(1L)).thenReturn(user);

        assertThrows(InvalidBookingStateException.class, () -> bookingService.cancelBooking(1L, 5L));
    }
}
