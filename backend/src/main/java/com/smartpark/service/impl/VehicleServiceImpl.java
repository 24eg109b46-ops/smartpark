package com.smartpark.service.impl;

import com.smartpark.dto.VehicleDto;
import com.smartpark.entity.User;
import com.smartpark.entity.Vehicle;
import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.exception.VehicleAlreadyExistsException;
import com.smartpark.repository.VehicleRepository;
import com.smartpark.service.UserService;
import com.smartpark.service.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    public VehicleServiceImpl(VehicleRepository vehicleRepository, UserService userService) {
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByUserId(Long userId) {
        return vehicleRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));
    }

    @Override
    public Vehicle addVehicle(Long userId, VehicleDto dto) {
        String cleanNumber = dto.getVehicleNumber().trim().toUpperCase();
        if (vehicleRepository.existsByVehicleNumber(cleanNumber)) {
            throw new VehicleAlreadyExistsException("A vehicle with license plate " + cleanNumber + " is already registered.");
        }

        User user = userService.findById(userId);
        Vehicle vehicle = new Vehicle(
                user,
                cleanNumber,
                dto.getVehicleType(),
                dto.getBrand() != null ? dto.getBrand().trim() : null,
                dto.getModel() != null ? dto.getModel().trim() : null
        );

        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(Long userId, Long vehicleId, VehicleDto dto) {
        Vehicle vehicle = getVehicleById(vehicleId);
        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to this vehicle");
        }

        String cleanNumber = dto.getVehicleNumber().trim().toUpperCase();
        if (vehicleRepository.existsByVehicleNumberAndIdNot(cleanNumber, vehicleId)) {
            throw new VehicleAlreadyExistsException("Another vehicle with license plate " + cleanNumber + " already exists.");
        }

        vehicle.setVehicleNumber(cleanNumber);
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setBrand(dto.getBrand() != null ? dto.getBrand().trim() : null);
        vehicle.setModel(dto.getModel() != null ? dto.getModel().trim() : null);

        return vehicleRepository.save(vehicle);
    }

    @Override
    public void deleteVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        if (!vehicle.getUser().getId().equals(userId)) {
            throw new BadRequestException("Unauthorized access to this vehicle");
        }
        vehicleRepository.delete(vehicle);
    }
}
