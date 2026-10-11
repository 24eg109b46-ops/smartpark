package com.smartpark.dto;

import com.smartpark.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BulkSlotCreateRequest {

    @NotNull(message = "Zone is required")
    private Long zoneId;

    @NotBlank(message = "Prefix is required (e.g. 'A-', 'B-', 'EV-')")
    private String prefix = "S-";

    @Min(value = 1, message = "Start number must be at least 1")
    private int startNumber = 1;

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Cannot create more than 100 slots at once")
    private int count = 10;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType = VehicleType.CAR;

    public BulkSlotCreateRequest() {
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getStartNumber() {
        return startNumber;
    }

    public void setStartNumber(int startNumber) {
        this.startNumber = startNumber;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }
}
