package com.smartpark.repository;

import com.smartpark.entity.ParkingZone;
import com.smartpark.enums.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {
    List<ParkingZone> findByStatus(ZoneStatus status);
}
