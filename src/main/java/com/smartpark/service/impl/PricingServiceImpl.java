package com.smartpark.service.impl;

import com.smartpark.dto.PricingRuleDto;
import com.smartpark.entity.ParkingZone;
import com.smartpark.entity.PricingRule;
import com.smartpark.enums.VehicleType;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.repository.ParkingZoneRepository;
import com.smartpark.repository.PricingRuleRepository;
import com.smartpark.service.PricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PricingServiceImpl implements PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ParkingZoneRepository parkingZoneRepository;

    public PricingServiceImpl(PricingRuleRepository pricingRuleRepository,
                              ParkingZoneRepository parkingZoneRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.parkingZoneRepository = parkingZoneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRule getMatchingRule(VehicleType vehicleType, Long zoneId) {
        if (zoneId != null) {
            var zoneSpecific = pricingRuleRepository.findByVehicleTypeAndZoneIdAndActiveTrue(vehicleType, zoneId);
            if (zoneSpecific.isPresent()) {
                return zoneSpecific.get();
            }
        }

        return pricingRuleRepository.findByVehicleTypeAndZoneIsNullAndActiveTrue(vehicleType)
                .orElseGet(() -> {
                    // Safe fallback default rule
                    PricingRule fallback = new PricingRule();
                    fallback.setVehicleType(vehicleType);
                    fallback.setRatePerHour(new BigDecimal("30.00"));
                    fallback.setMinCharge(new BigDecimal("30.00"));
                    fallback.setFreeMinutes(15);
                    fallback.setActive(true);
                    return fallback;
                });
    }

    @Override
    public BigDecimal calculatePrice(VehicleType vehicleType, Long zoneId, LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null || exitTime.isBefore(entryTime)) {
            exitTime = entryTime != null ? entryTime.plusHours(1) : LocalDateTime.now();
        }

        PricingRule rule = getMatchingRule(vehicleType, zoneId);

        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        if (totalMinutes < 0) {
            totalMinutes = 0;
        }

        long netMinutes = totalMinutes - rule.getFreeMinutes();
        long billableHours = 0;
        if (netMinutes > 0) {
            billableHours = (long) Math.ceil((double) netMinutes / 60.0);
        }

        BigDecimal calculatedAmount = rule.getRatePerHour().multiply(BigDecimal.valueOf(billableHours));
        BigDecimal finalAmount = calculatedAmount.max(rule.getMinCharge());

        return finalAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateEstimatedPrice(VehicleType vehicleType, Long zoneId, int durationHours) {
        if (durationHours <= 0) {
            durationHours = 1;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exit = now.plusHours(durationHours);
        return calculatePrice(vehicleType, zoneId, now, exit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRuleDto> getAllRules() {
        return pricingRuleRepository.findAllByOrderByVehicleTypeAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRuleDto getRuleById(Long id) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with ID: " + id));
        return toDto(rule);
    }

    @Override
    public PricingRuleDto createRule(PricingRuleDto dto) {
        PricingRule rule = new PricingRule();
        updateEntityFromDto(rule, dto);
        PricingRule saved = pricingRuleRepository.save(rule);
        return toDto(saved);
    }

    @Override
    public PricingRuleDto updateRule(Long id, PricingRuleDto dto) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with ID: " + id));
        updateEntityFromDto(rule, dto);
        PricingRule saved = pricingRuleRepository.save(rule);
        return toDto(saved);
    }

    @Override
    public void deleteRule(Long id) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with ID: " + id));
        pricingRuleRepository.delete(rule);
    }

    private void updateEntityFromDto(PricingRule entity, PricingRuleDto dto) {
        entity.setVehicleType(dto.getVehicleType());
        entity.setRatePerHour(dto.getRatePerHour());
        entity.setMinCharge(dto.getMinCharge());
        entity.setFreeMinutes(dto.getFreeMinutes());
        entity.setActive(dto.isActive());
        if (dto.getZoneId() != null) {
            ParkingZone zone = parkingZoneRepository.findById(dto.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + dto.getZoneId()));
            entity.setZone(zone);
        } else {
            entity.setZone(null);
        }
    }

    private PricingRuleDto toDto(PricingRule rule) {
        Long zoneId = rule.getZone() != null ? rule.getZone().getId() : null;
        String zoneName = rule.getZone() != null ? rule.getZone().getName() : "Global (All Zones)";
        return new PricingRuleDto(
                rule.getId(),
                rule.getVehicleType(),
                zoneId,
                zoneName,
                rule.getRatePerHour(),
                rule.getMinCharge(),
                rule.getFreeMinutes(),
                rule.isActive()
        );
    }
}
