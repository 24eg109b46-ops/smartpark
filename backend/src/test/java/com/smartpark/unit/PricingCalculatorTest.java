package com.smartpark.unit;

import com.smartpark.entity.ParkingZone;
import com.smartpark.entity.PricingRule;
import com.smartpark.enums.VehicleType;
import com.smartpark.enums.ZoneStatus;
import com.smartpark.repository.ParkingZoneRepository;
import com.smartpark.repository.PricingRuleRepository;
import com.smartpark.service.impl.PricingServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingCalculatorTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private ParkingZoneRepository parkingZoneRepository;

    @InjectMocks
    private PricingServiceImpl pricingService;

    private ParkingZone testZone;
    private PricingRule globalCarRule;
    private PricingRule zoneCarRule;

    @BeforeEach
    void setUp() {
        testZone = new ParkingZone("Zone A", "Terminal 1", "Ground", 20, ZoneStatus.ACTIVE);
        testZone.setId(1L);

        // Global Car Rule: $20/hr, $20 min charge, 15 min free
        globalCarRule = new PricingRule(VehicleType.CAR, null, new BigDecimal("20.00"), new BigDecimal("20.00"), 15, true);

        // Zone 1 Car Rule: $30/hr, $30 min charge, 10 min free
        zoneCarRule = new PricingRule(VehicleType.CAR, testZone, new BigDecimal("30.00"), new BigDecimal("30.00"), 10, true);
    }

    @Test
    @DisplayName("Should apply minimum charge when duration is less than or within free grace minutes")
    void testMinimumChargeWithinFreeMinutes() {
        when(pricingRuleRepository.findByVehicleTypeAndZoneIsNullAndActiveTrue(VehicleType.CAR))
                .thenReturn(Optional.of(globalCarRule));

        LocalDateTime entry = LocalDateTime.now();
        LocalDateTime exit = entry.plusMinutes(10); // 10 mins <= 15 free mins

        BigDecimal price = pricingService.calculatePrice(VehicleType.CAR, null, entry, exit);
        assertEquals(new BigDecimal("20.00"), price);
    }

    @Test
    @DisplayName("Should round up (ceil) hours after subtracting free minutes")
    void testCeilRoundingAfterFreeMinutes() {
        when(pricingRuleRepository.findByVehicleTypeAndZoneIsNullAndActiveTrue(VehicleType.CAR))
                .thenReturn(Optional.of(globalCarRule));

        LocalDateTime entry = LocalDateTime.now();
        // 70 minutes elapsed: net = 70 - 15 = 55 minutes -> ceil = 1 hour -> 1 * 20 = $20.00
        LocalDateTime exit = entry.plusMinutes(70);

        BigDecimal price = pricingService.calculatePrice(VehicleType.CAR, null, entry, exit);
        assertEquals(new BigDecimal("20.00"), price);

        // 80 minutes elapsed: net = 80 - 15 = 65 minutes -> ceil = 2 hours -> 2 * 20 = $40.00
        exit = entry.plusMinutes(80);
        price = pricingService.calculatePrice(VehicleType.CAR, null, entry, exit);
        assertEquals(new BigDecimal("40.00"), price);
    }

    @Test
    @DisplayName("Zone-specific pricing rule should override global rule")
    void testZoneSpecificRuleOverride() {
        when(pricingRuleRepository.findByVehicleTypeAndZoneIdAndActiveTrue(VehicleType.CAR, 1L))
                .thenReturn(Optional.of(zoneCarRule));

        LocalDateTime entry = LocalDateTime.now();
        // 75 minutes: net = 75 - 10 = 65 mins -> ceil = 2 hours -> 2 * 30 = $60.00
        LocalDateTime exit = entry.plusMinutes(75);

        BigDecimal price = pricingService.calculatePrice(VehicleType.CAR, 1L, entry, exit);
        assertEquals(new BigDecimal("60.00"), price);
    }
}
