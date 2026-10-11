package com.smartpark.dto;

import com.smartpark.enums.ZoneStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ZoneDto {

    private Long id;

    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 50, message = "Floor description cannot exceed 50 characters")
    private String floor;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity = 20;

    private ZoneStatus status = ZoneStatus.ACTIVE;

    public ZoneDto() {
    }

    public ZoneDto(Long id, String name, String location, String floor, Integer capacity, ZoneStatus status) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.floor = floor;
        this.capacity = capacity;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public ZoneStatus getStatus() {
        return status;
    }

    public void setStatus(ZoneStatus status) {
        this.status = status;
    }
}
