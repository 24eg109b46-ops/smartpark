package com.smartpark.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class BookingRequest {

    @NotNull(message = "Parking slot is required")
    private Long slotId;

    @NotNull(message = "Vehicle selection is required")
    private Long vehicleId;

    @NotNull(message = "Entry time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime entryTime;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Minimum duration is 1 hour")
    @Max(value = 72, message = "Maximum duration is 72 hours")
    private Integer durationHours = 2;

    public BookingRequest() {
    }

    public BookingRequest(Long slotId, Long vehicleId, LocalDateTime entryTime, Integer durationHours) {
        this.slotId = slotId;
        this.vehicleId = vehicleId;
        this.entryTime = entryTime;
        this.durationHours = durationHours;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }
}
