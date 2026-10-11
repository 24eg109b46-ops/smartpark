package com.smartpark.dto;

import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SlotDto {

    private Long id;

    @NotNull(message = "Zone is required")
    private Long zoneId;

    private String zoneName;

    @NotBlank(message = "Slot number is required")
    @Size(max = 20, message = "Slot number cannot exceed 20 characters")
    private String slotNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType = VehicleType.CAR;

    @NotNull(message = "Status is required")
    private SlotStatus status = SlotStatus.AVAILABLE;

    public SlotDto() {
    }

    public SlotDto(Long id, Long zoneId, String zoneName, String slotNumber, VehicleType vehicleType, SlotStatus status) {
        this.id = id;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.slotNumber = slotNumber;
        this.vehicleType = vehicleType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber != null ? slotNumber.trim().toUpperCase() : null;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }
}
