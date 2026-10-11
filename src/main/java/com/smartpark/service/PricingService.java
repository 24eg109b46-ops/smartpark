package com.smartpark.service;

import com.smartpark.dto.PricingRuleDto;
import com.smartpark.entity.Booking;
import com.smartpark.entity.PricingRule;
import com.smartpark.enums.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PricingService {

    PricingRule getMatchingRule(VehicleType vehicleType, Long zoneId);

    BigDecimal calculatePrice(VehicleType vehicleType, Long zoneId, LocalDateTime entryTime, LocalDateTime exitTime);

    BigDecimal calculateEstimatedPrice(VehicleType vehicleType, Long zoneId, int durationHours);

    List<PricingRuleDto> getAllRules();

    PricingRuleDto getRuleById(Long id);

    PricingRuleDto createRule(PricingRuleDto dto);

    PricingRuleDto updateRule(Long id, PricingRuleDto dto);

    void deleteRule(Long id);
}
