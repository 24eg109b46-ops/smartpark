package com.smartpark.repository;

import com.smartpark.entity.User;
import com.smartpark.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByUser(User user);
    List<Vehicle> findByUserId(Long userId);
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
    boolean existsByVehicleNumber(String vehicleNumber);
    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);
}
