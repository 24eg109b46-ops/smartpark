package com.smartpark.integration;

import com.smartpark.entity.Booking;
import com.smartpark.entity.ParkingSlot;
import com.smartpark.entity.Vehicle;
import com.smartpark.enums.BookingStatus;
import com.smartpark.enums.SlotStatus;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.ParkingSlotRepository;
import com.smartpark.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BookingFlowMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @WithMockUser(username = "driver@smartpark.com", authorities = {"ROLE_DRIVER"})
    @DisplayName("Driver can view dashboard, search slots, and reserve a slot")
    void testDriverReserveFlow() throws Exception {
        // 1. View Dashboard
        mockMvc.perform(get("/driver/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/dashboard"));

        // 2. View Search
        mockMvc.perform(get("/driver/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/search"));

        // 3. Find an available slot
        ParkingSlot slot = parkingSlotRepository.findAll().stream()
                .filter(s -> s.getStatus() == SlotStatus.AVAILABLE && s.getVehicleType() == com.smartpark.enums.VehicleType.CAR)
                .findFirst()
                .orElseThrow();

        Vehicle vehicle = vehicleRepository.findAll().stream()
                .filter(v -> v.getVehicleType() == com.smartpark.enums.VehicleType.CAR)
                .findFirst()
                .orElseThrow();

        LocalDateTime entry = LocalDateTime.now().plusHours(1);

        // 4. Reserve Slot
        mockMvc.perform(post("/driver/reserve")
                .param("slotId", slot.getId().toString())
                .param("vehicleId", vehicle.getId().toString())
                .param("entryTime", entry.toString())
                .param("durationHours", "2")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/driver/bookings/*"));
    }

    @Test
    @WithMockUser(username = "operator@smartpark.com", authorities = {"ROLE_OPERATOR"})
    @DisplayName("Operator can view bookings, process check-in and check-out")
    void testOperatorCheckInAndCheckOut() throws Exception {
        // Find an upcoming or active booking
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .findFirst()
                .orElse(null);

        if (booking != null) {
            // Operator checks in
            mockMvc.perform(post("/operator/bookings/check-in/" + booking.getId())
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/operator/bookings"));

            Booking refreshed = bookingRepository.findById(booking.getId()).orElseThrow();
            assertEquals(BookingStatus.ACTIVE, refreshed.getStatus());

            // Operator checks out
            mockMvc.perform(post("/operator/bookings/check-out/" + booking.getId())
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/operator/bookings"));

            refreshed = bookingRepository.findById(booking.getId()).orElseThrow();
            assertEquals(BookingStatus.COMPLETED, refreshed.getStatus());
        }
    }
}
