package com.smartpark.repository;

import com.smartpark.entity.PricingRule;
import com.smartpark.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findByVehicleTypeAndZoneIdAndActiveTrue(VehicleType vehicleType, Long zoneId);

    Optional<PricingRule> findByVehicleTypeAndZoneIsNullAndActiveTrue(VehicleType vehicleType);

    List<PricingRule> findByActiveTrue();

    List<PricingRule> findAllByOrderByVehicleTypeAsc();
}
