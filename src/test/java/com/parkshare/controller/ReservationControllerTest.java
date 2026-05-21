package com.parkshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkshare.dto.CreateReservationRequest;
import com.parkshare.dto.ReservationResponse;
import com.parkshare.exception.SpaceNotAvailableException;
import com.parkshare.security.JwtAuthFilter;
import com.parkshare.security.JwtService;
import com.parkshare.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldCreateReservationSuccessfully() throws Exception {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setParkingSpaceId(1L);

        ReservationResponse response = new ReservationResponse();
        response.setId(100L);
        response.setStatus("PENDING");

        when(reservationService.createReservation(any(Long.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetMyReservations() throws Exception {
        when(reservationService.getMyReservations()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/reservations/my"))
                .andExpect(status().isOk());
    }
}
