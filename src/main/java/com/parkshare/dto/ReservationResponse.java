package com.parkshare.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para una reserva.
 *
 * Incluye objetos anidados ParkingSpaceInfo (sin datos sensibles del host)
 * y DriverInfo (sin password) para evitar exponer entidades JPA directamente.
 */
@Data
public class ReservationResponse {

    private Long id;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expiresAt;

    /** Información resumida de la cochera reservada */
    private ParkingSpaceInfo parkingSpace;

    /** Información resumida del conductor que reservó */
    private DriverInfo driver;

    // ==================== DTOs Anidados ====================

    /**
     * Info básica de la cochera — se incluye en la respuesta de reserva
     * para que el frontend muestre datos sin hacer una segunda llamada.
     */
    @Data
    public static class ParkingSpaceInfo {
        private Long id;
        private String title;
        private String address;
        private BigDecimal pricePerHour;
    }

    /**
     * Info del conductor — NUNCA incluye el password.
     * Solo expone datos necesarios para la interfaz.
     */
    @Data
    public static class DriverInfo {
        private Long id;
        private String name;
        private String email;
    }
}
