package com.parkshare.controller;

import com.parkshare.dto.CheckInRequest;
import com.parkshare.dto.CheckInResponse;
import com.parkshare.dto.CheckOutRequest;
import com.parkshare.dto.CheckOutResponse;
import com.parkshare.dto.QRResponse;
import com.parkshare.service.CheckInOutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/check-in-out")
@RequiredArgsConstructor
public class CheckInOutController {

    private final CheckInOutService checkInOutService;

    @PostMapping("/generate-qr/{reservationId}")
    public ResponseEntity<QRResponse> generateQR(@PathVariable Long reservationId) {
        return ResponseEntity.ok(checkInOutService.generateQR(reservationId));
    }

    @PostMapping("/process-check-in")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(checkInOutService.checkIn(request.getCode()));
    }

    @PostMapping("/process-check-out")
    public ResponseEntity<CheckOutResponse> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(checkInOutService.checkOut(request.getCode()));
    }
}
