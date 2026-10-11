package com.smartpark.dto;

import com.smartpark.enums.VehicleType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class SlotSearchRequest {

    private Long zoneId;
    private VehicleType vehicleType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime entryTime;

    private Integer durationHours = 2;
    private String location;

    public SlotSearchRequest() {
        this.entryTime = LocalDateTime.now().plusMinutes(5);
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
