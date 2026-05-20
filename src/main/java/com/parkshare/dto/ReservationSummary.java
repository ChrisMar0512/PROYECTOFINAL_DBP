package com.parkshare.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resumen de una reserva para usar en el dashboard del host.
 *
 * Contiene solo los campos esenciales para una vista rápida,
 * sin exponer datos sensibles del driver ni la entidad completa.
 */
@Data
public class ReservationSummary {

    private Long id;
    private String status;
    private LocalDateTime reservedAt;

    /** Nombre del conductor que realizó la reserva */
    private String driverName;
}
