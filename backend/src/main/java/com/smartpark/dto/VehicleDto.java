package com.smartpark.dto;

import com.smartpark.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VehicleDto {

    private Long id;

    @NotBlank(message = "Vehicle number plate is required")
    @Pattern(regexp = "^[A-Z0-9 -]{3,20}$", message = "Vehicle plate must contain 3-20 uppercase alphanumeric characters")
    private String vehicleNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType = VehicleType.CAR;

    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    private String brand;

    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    public VehicleDto() {
    }

    public VehicleDto(Long id, String vehicleNumber, VehicleType vehicleType, String brand, String model) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.brand = brand;
        this.model = model;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber != null ? vehicleNumber.toUpperCase().trim() : null;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
