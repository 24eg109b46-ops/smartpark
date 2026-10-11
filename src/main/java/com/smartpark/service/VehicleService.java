package com.smartpark.service;

import com.smartpark.dto.VehicleDto;
import com.smartpark.entity.Vehicle;

import java.util.List;

public interface VehicleService {

    List<Vehicle> getVehiclesByUserId(Long userId);

    Vehicle getVehicleById(Long id);

    Vehicle addVehicle(Long userId, VehicleDto dto);

    Vehicle updateVehicle(Long userId, Long vehicleId, VehicleDto dto);

    void deleteVehicle(Long userId, Long vehicleId);
}
