package com.smartpark.service.impl;

import com.smartpark.dto.BulkSlotCreateRequest;
import com.smartpark.dto.SlotDto;
import com.smartpark.dto.SlotSearchRequest;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.entity.ParkingZone;
import com.smartpark.enums.BookingStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.ZoneStatus;
import com.smartpark.exception.BadRequestException;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.ParkingZoneRepository;
import com.smartpark.service.ParkingSlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParkingSlotServiceImpl implements ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final BookingRepository bookingRepository;

    public ParkingSlotServiceImpl(ParkingSlotRepository parkingSlotRepository,
                                  ParkingZoneRepository parkingZoneRepository,
                                  BookingRepository bookingRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.parkingZoneRepository = parkingZoneRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAllSlots() {
        return parkingSlotRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> getSlotsByZone(Long zoneId) {
        return parkingSlotRepository.findByZoneIdOrderBySlotNumberAsc(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSlot getSlotById(Long id) {
        return parkingSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found with ID: " + id));
    }

    @Override
    public ParkingSlot createSlot(SlotDto dto) {
        ParkingZone zone = parkingZoneRepository.findById(dto.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking zone not found with ID: " + dto.getZoneId()));

        String slotNum = dto.getSlotNumber().trim().toUpperCase();
        if (parkingSlotRepository.existsByZoneIdAndSlotNumber(zone.getId(), slotNum)) {
            throw new BadRequestException("Slot " + slotNum + " already exists in " + zone.getName());
        }

        ParkingSlot slot = new ParkingSlot(zone, slotNum, dto.getVehicleType(), dto.getStatus());
        return parkingSlotRepository.save(slot);
    }

    @Override
    public int bulkCreateSlots(BulkSlotCreateRequest request) {
        ParkingZone zone = parkingZoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking zone not found with ID: " + request.getZoneId()));

        int created = 0;
        List<ParkingSlot> toSave = new ArrayList<>();
        String prefix = request.getPrefix() != null ? request.getPrefix().trim().toUpperCase() : "S-";

        for (int i = 0; i < request.getCount(); i++) {
            int slotNumIndex = request.getStartNumber() + i;
            String slotNum = prefix + String.format("%02d", slotNumIndex);

            if (!parkingSlotRepository.existsByZoneIdAndSlotNumber(zone.getId(), slotNum)) {
                ParkingSlot slot = new ParkingSlot(zone, slotNum, request.getVehicleType(), SlotStatus.AVAILABLE);
                toSave.add(slot);
                created++;
            }
        }

        if (!toSave.isEmpty()) {
            parkingSlotRepository.saveAll(toSave);
        }

        return created;
    }

    @Override
    public ParkingSlot updateSlotStatus(Long slotId, SlotStatus status) {
        ParkingSlot slot = getSlotById(slotId);
        slot.setStatus(status);
        return parkingSlotRepository.save(slot);
    }

    @Override
    public void deleteSlot(Long slotId) {
        ParkingSlot slot = getSlotById(slotId);
        parkingSlotRepository.delete(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> searchAvailableSlots(SlotSearchRequest request) {
        LocalDateTime entry = request.getEntryTime() != null ? request.getEntryTime() : LocalDateTime.now();
        int duration = (request.getDurationHours() != null && request.getDurationHours() > 0) ? request.getDurationHours() : 2;
        LocalDateTime exit = entry.plusHours(duration);

        List<ParkingSlot> candidates = parkingSlotRepository.findAll().stream()
                .filter(s -> s.getZone().getStatus() == ZoneStatus.ACTIVE)
                .filter(s -> s.getStatus() != SlotStatus.MAINTENANCE)
                .filter(s -> request.getZoneId() == null || s.getZone().getId().equals(request.getZoneId()))
                .filter(s -> request.getVehicleType() == null || s.getVehicleType() == request.getVehicleType())
                .filter(s -> request.getLocation() == null || request.getLocation().isBlank() ||
                        s.getZone().getLocation().toLowerCase().contains(request.getLocation().trim().toLowerCase()))
                .collect(Collectors.toList());

        List<BookingStatus> blockingStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

        return candidates.stream().filter(slot -> {
            // Check if slot has overlapping bookings
            List<?> overlapping = bookingRepository.findOverlappingBookings(slot.getId(), entry, exit, blockingStatuses);
            if (!overlapping.isEmpty()) {
                return false;
            }

            // If reservation starts right now (within 30 mins), ensure slot is not currently OCCUPIED or RESERVED
            LocalDateTime now = LocalDateTime.now();
            if (entry.isBefore(now.plusMinutes(30)) && entry.isAfter(now.minusMinutes(30))) {
                if (slot.getStatus() == SlotStatus.OCCUPIED || slot.getStatus() == SlotStatus.RESERVED) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalSlots() {
        return parkingSlotRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSlotsByStatus(SlotStatus status) {
        return parkingSlotRepository.countByStatus(status);
    }
}
