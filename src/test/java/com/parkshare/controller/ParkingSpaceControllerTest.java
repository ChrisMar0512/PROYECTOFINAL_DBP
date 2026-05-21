package com.parkshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkshare.dto.ParkingSpaceResponse;
import com.parkshare.security.JwtAuthFilter;
import com.parkshare.security.JwtService;
import com.parkshare.service.ParkingSpaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingSpaceService parkingSpaceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void shouldGetMyParkingSpaces() throws Exception {
        when(parkingSpaceService.getMyParkingSpaces()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/parking-spaces/my-published-spaces")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
