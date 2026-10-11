package com.smartpark.service.impl;

import com.smartpark.dto.ZoneDto;
import com.smartpark.entity.ParkingZone;
import com.smartpark.enums.ZoneStatus;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.repository.ParkingZoneRepository;
import com.smartpark.service.ParkingZoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ParkingZoneServiceImpl implements ParkingZoneService {

    private final ParkingZoneRepository parkingZoneRepository;

    public ParkingZoneServiceImpl(ParkingZoneRepository parkingZoneRepository) {
        this.parkingZoneRepository = parkingZoneRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingZone> getAllZones() {
        return parkingZoneRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingZone> getActiveZones() {
        return parkingZoneRepository.findByStatus(ZoneStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingZone getZoneById(Long id) {
        return parkingZoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking zone not found with ID: " + id));
    }

    @Override
    public ParkingZone createZone(ZoneDto dto) {
        ParkingZone zone = new ParkingZone(
                dto.getName().trim(),
                dto.getLocation().trim(),
                dto.getFloor() != null ? dto.getFloor().trim() : null,
                dto.getCapacity(),
                dto.getStatus() != null ? dto.getStatus() : ZoneStatus.ACTIVE
        );
        return parkingZoneRepository.save(zone);
    }

    @Override
    public ParkingZone updateZone(Long id, ZoneDto dto) {
        ParkingZone zone = getZoneById(id);
        zone.setName(dto.getName().trim());
        zone.setLocation(dto.getLocation().trim());
        zone.setFloor(dto.getFloor() != null ? dto.getFloor().trim() : null);
        zone.setCapacity(dto.getCapacity());
        if (dto.getStatus() != null) {
            zone.setStatus(dto.getStatus());
        }
        return parkingZoneRepository.save(zone);
    }

    @Override
    public void deleteZone(Long id) {
        ParkingZone zone = getZoneById(id);
        parkingZoneRepository.delete(zone);
    }
}
