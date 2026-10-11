package com.smartpark.repository;

import com.smartpark.entity.ParkingSlot;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByZoneId(Long zoneId);

    List<ParkingSlot> findByZoneIdOrderBySlotNumberAsc(Long zoneId);

    List<ParkingSlot> findByStatus(SlotStatus status);

    List<ParkingSlot> findByZoneIdAndStatus(Long zoneId, SlotStatus status);

    List<ParkingSlot> findByVehicleTypeAndStatus(VehicleType vehicleType, SlotStatus status);

    List<ParkingSlot> findByZoneIdAndVehicleTypeAndStatus(Long zoneId, VehicleType vehicleType, SlotStatus status);

    Optional<ParkingSlot> findByZoneIdAndSlotNumber(Long zoneId, String slotNumber);

    boolean existsByZoneIdAndSlotNumber(Long zoneId, String slotNumber);

    long countByStatus(SlotStatus status);

    long countByZoneIdAndStatus(Long zoneId, SlotStatus status);

    long countByZoneId(Long zoneId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSlot s WHERE s.id = :id")
    Optional<ParkingSlot> findByIdWithPessimisticLock(@Param("id") Long id);
}
