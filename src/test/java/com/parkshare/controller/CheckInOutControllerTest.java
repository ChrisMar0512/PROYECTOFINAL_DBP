package com.parkshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkshare.dto.CheckInRequest;
import com.parkshare.dto.CheckInResponse;
import com.parkshare.dto.CheckOutRequest;
import com.parkshare.dto.CheckOutResponse;
import com.parkshare.dto.QRResponse;
import com.parkshare.security.JwtAuthFilter;
import com.parkshare.security.JwtService;
import com.parkshare.service.CheckInOutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInOutController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckInOutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CheckInOutService checkInOutService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldGenerateQRSuccessfully() throws Exception {
        QRResponse response = new QRResponse();
        response.setCode("test-qr-code");

        when(checkInOutService.generateQR(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/generate-qr/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("test-qr-code"));
    }

    @Test
    void shouldCheckInSuccessfully() throws Exception {
        CheckInRequest request = new CheckInRequest();
        request.setCode("test-code");

        CheckInResponse response = new CheckInResponse();
        response.setMessage("Check-in exitoso");

        when(checkInOutService.checkIn(eq("test-code"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/process-check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Check-in exitoso"));
    }

    @Test
    void shouldCheckOutSuccessfully() throws Exception {
        CheckOutRequest request = new CheckOutRequest();
        request.setCode("test-code");

        CheckOutResponse response = new CheckOutResponse();
        response.setDurationMinutes(60L);

        when(checkInOutService.checkOut(eq("test-code"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/check-in-out/process-check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60));
    }
}
