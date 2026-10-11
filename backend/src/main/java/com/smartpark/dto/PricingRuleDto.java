package com.smartpark.dto;

import com.smartpark.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PricingRuleDto {

    private Long id;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType = VehicleType.CAR;

    private Long zoneId;
    private String zoneName;

    @NotNull(message = "Rate per hour is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must be positive")
    private BigDecimal ratePerHour;

    @NotNull(message = "Minimum charge is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum charge must be positive")
    private BigDecimal minCharge;

    @Min(value = 0, message = "Free minutes cannot be negative")
    private int freeMinutes = 15;

    private boolean active = true;

    public PricingRuleDto() {
    }

    public PricingRuleDto(Long id, VehicleType vehicleType, Long zoneId, String zoneName,
                          BigDecimal ratePerHour, BigDecimal minCharge, int freeMinutes, boolean active) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.ratePerHour = ratePerHour;
        this.minCharge = minCharge;
        this.freeMinutes = freeMinutes;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public BigDecimal getRatePerHour() {
        return ratePerHour;
    }

    public void setRatePerHour(BigDecimal ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    public BigDecimal getMinCharge() {
        return minCharge;
    }

    public void setMinCharge(BigDecimal minCharge) {
        this.minCharge = minCharge;
    }

    public int getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(int freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
