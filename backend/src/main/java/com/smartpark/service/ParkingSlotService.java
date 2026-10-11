package com.smartpark.service;

import com.smartpark.dto.BulkSlotCreateRequest;
import com.smartpark.dto.SlotDto;
import com.smartpark.dto.SlotSearchRequest;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.enums.SlotStatus;

import java.util.List;

public interface ParkingSlotService {

    List<ParkingSlot> getAllSlots();

    List<ParkingSlot> getSlotsByZone(Long zoneId);

    ParkingSlot getSlotById(Long id);

    ParkingSlot createSlot(SlotDto dto);

    int bulkCreateSlots(BulkSlotCreateRequest request);

    ParkingSlot updateSlotStatus(Long slotId, SlotStatus status);

    void deleteSlot(Long slotId);

    List<ParkingSlot> searchAvailableSlots(SlotSearchRequest request);

    long countTotalSlots();

    long countSlotsByStatus(SlotStatus status);
}
