package com.smartpark.config;

import com.smartpark.entity.*;
import com.smartpark.enums.*;
import com.smartpark.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      VehicleRepository vehicleRepository,
                      ParkingZoneRepository parkingZoneRepository,
                      ParkingSlotRepository parkingSlotRepository,
                      PricingRuleRepository pricingRuleRepository,
                      BookingRepository bookingRepository,
                      PaymentRepository paymentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingZoneRepository = parkingZoneRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Starting initial SmartPark database seeding...");

        // 1. Seed Users
        User admin = new User("System Admin", "admin@smartpark.com", "+1-555-0100", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN, UserStatus.ACTIVE);
        User operator = new User("Lead Operator", "operator@smartpark.com", "+1-555-0101", passwordEncoder.encode("Operator@123"), Role.ROLE_OPERATOR, UserStatus.ACTIVE);
        User driver = new User("John Driver", "driver@smartpark.com", "+1-555-0102", passwordEncoder.encode("Driver@123"), Role.ROLE_DRIVER, UserStatus.ACTIVE);

        userRepository.save(admin);
        userRepository.save(operator);
        driver = userRepository.save(driver);

        // 2. Seed Vehicles for Driver
        Vehicle car = new Vehicle(driver, "KA-01-AB-1234", VehicleType.CAR, "Honda", "Civic");
        Vehicle ev = new Vehicle(driver, "KA-02-EV-5678", VehicleType.EV, "Tesla", "Model 3");
        Vehicle bike = new Vehicle(driver, "KA-03-BK-9999", VehicleType.BIKE, "Yamaha", "R15");

        vehicleRepository.save(car);
        vehicleRepository.save(ev);
        vehicleRepository.save(bike);

        // 3. Seed Parking Zones
        ParkingZone zoneA = new ParkingZone("Zone A - Ground Floor North", "Terminal 1 Gate 3", "Ground (Floor 0)", 20, ZoneStatus.ACTIVE);
        ParkingZone zoneB = new ParkingZone("Zone B - Underground EV Plaza", "Basement Plaza", "Basement B1", 15, ZoneStatus.ACTIVE);
        ParkingZone zoneC = new ParkingZone("Zone C - Multi-Level East Deck", "East Wing Deck", "Floor 2", 20, ZoneStatus.ACTIVE);

        zoneA = parkingZoneRepository.save(zoneA);
        zoneB = parkingZoneRepository.save(zoneB);
        zoneC = parkingZoneRepository.save(zoneC);

        // 4. Seed Slots across Vehicle Types (47 slots total)
        // Zone A: 12 CAR slots, 4 TRUCK slots
        for (int i = 1; i <= 12; i++) {
            ParkingSlot slot = new ParkingSlot(zoneA, String.format("A-%02d", i), VehicleType.CAR, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }
        for (int i = 13; i <= 16; i++) {
            ParkingSlot slot = new ParkingSlot(zoneA, String.format("A-%02d", i), VehicleType.TRUCK, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }

        // Zone B: 10 EV slots, 5 CAR slots
        for (int i = 1; i <= 10; i++) {
            ParkingSlot slot = new ParkingSlot(zoneB, String.format("B-%02d", i), VehicleType.EV, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }
        for (int i = 11; i <= 15; i++) {
            ParkingSlot slot = new ParkingSlot(zoneB, String.format("B-%02d", i), VehicleType.CAR, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }

        // Zone C: 10 BIKE slots, 6 CAR slots
        for (int i = 1; i <= 10; i++) {
            ParkingSlot slot = new ParkingSlot(zoneC, String.format("C-%02d", i), VehicleType.BIKE, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }
        for (int i = 11; i <= 16; i++) {
            ParkingSlot slot = new ParkingSlot(zoneC, String.format("C-%02d", i), VehicleType.CAR, SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);
        }

        // 5. Seed Pricing Rules
        PricingRule ruleCar = new PricingRule(VehicleType.CAR, null, new BigDecimal("25.00"), new BigDecimal("25.00"), 15, true);
        PricingRule ruleBike = new PricingRule(VehicleType.BIKE, null, new BigDecimal("10.00"), new BigDecimal("10.00"), 15, true);
        PricingRule ruleEv = new PricingRule(VehicleType.EV, null, new BigDecimal("30.00"), new BigDecimal("30.00"), 15, true);
        PricingRule ruleTruck = new PricingRule(VehicleType.TRUCK, null, new BigDecimal("50.00"), new BigDecimal("50.00"), 15, true);
        PricingRule ruleZoneBEv = new PricingRule(VehicleType.EV, zoneB, new BigDecimal("35.00"), new BigDecimal("35.00"), 15, true);

        pricingRuleRepository.save(ruleCar);
        pricingRuleRepository.save(ruleBike);
        pricingRuleRepository.save(ruleEv);
        pricingRuleRepository.save(ruleTruck);
        pricingRuleRepository.save(ruleZoneBEv);

        // 6. Seed Sample Bookings
        ParkingSlot slotA1 = parkingSlotRepository.findByZoneIdAndSlotNumber(zoneA.getId(), "A-01").orElseThrow();
        ParkingSlot slotB1 = parkingSlotRepository.findByZoneIdAndSlotNumber(zoneB.getId(), "B-01").orElseThrow();
        ParkingSlot slotC1 = parkingSlotRepository.findByZoneIdAndSlotNumber(zoneC.getId(), "C-01").orElseThrow();

        // Historical completed booking
        LocalDateTime histEntry = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0);
        LocalDateTime histExit = histEntry.plusHours(2);
        Booking bookingCompleted = new Booking("SP-HIST001", driver, car, slotA1, histEntry, 2, BookingStatus.COMPLETED, new BigDecimal("50.00"));
        bookingCompleted.setExitTime(histExit);
        bookingCompleted = bookingRepository.save(bookingCompleted);

        Payment paymentCompleted = new Payment(bookingCompleted, new BigDecimal("50.00"), PaymentMode.ONLINE, "TRX-HIST001", PaymentStatus.PAID);
        paymentCompleted.setPaidAt(histExit);
        paymentRepository.save(paymentCompleted);

        // Active booking (in progress)
        LocalDateTime actEntry = LocalDateTime.now().minusHours(1);
        LocalDateTime actExit = actEntry.plusHours(3);
        Booking bookingActive = new Booking("SP-ACT0002", driver, ev, slotB1, actEntry, 3, BookingStatus.ACTIVE, new BigDecimal("105.00"));
        bookingActive.setExitTime(actExit);
        bookingActive = bookingRepository.save(bookingActive);
        slotB1.setStatus(SlotStatus.OCCUPIED);
        parkingSlotRepository.save(slotB1);

        Payment paymentActive = new Payment(bookingActive, new BigDecimal("105.00"), PaymentMode.ONLINE, "TRX-ACT0002", PaymentStatus.PENDING);
        paymentRepository.save(paymentActive);

        // Upcoming confirmed booking
        LocalDateTime upEntry = LocalDateTime.now().plusHours(4);
        LocalDateTime upExit = upEntry.plusHours(2);
        Booking bookingUpcoming = new Booking("SP-UPC0003", driver, bike, slotC1, upEntry, 2, BookingStatus.CONFIRMED, new BigDecimal("20.00"));
        bookingUpcoming.setExitTime(upExit);
        bookingUpcoming = bookingRepository.save(bookingUpcoming);

        Payment paymentUpcoming = new Payment(bookingUpcoming, new BigDecimal("20.00"), PaymentMode.ONLINE, "TRX-UPC0003", PaymentStatus.PENDING);
        paymentRepository.save(paymentUpcoming);

        log.info("SmartPark initial database seeding completed successfully with 3 users, 3 zones, 47 slots, and sample bookings!");
    }
}
