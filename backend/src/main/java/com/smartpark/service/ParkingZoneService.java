package com.smartpark.service;

import com.smartpark.dto.ZoneDto;
import com.smartpark.entity.ParkingZone;

import java.util.List;

public interface ParkingZoneService {

    List<ParkingZone> getAllZones();

    List<ParkingZone> getActiveZones();

    ParkingZone getZoneById(Long id);

    ParkingZone createZone(ZoneDto dto);

    ParkingZone updateZone(Long id, ZoneDto dto);

    void deleteZone(Long id);
}
