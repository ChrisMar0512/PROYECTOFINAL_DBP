package com.parkshare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento enviado vía WebSocket al topic /topic/parking-updates cuando
 * el estado de una cochera cambia (por reserva, expiración, cancelación, etc.).
 *
 * Los clientes suscritos a este topic reciben actualizaciones en tiempo real
 * para refrescar el mapa de cocheras disponibles sin hacer polling.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingUpdateEvent {

    /** ID de la cochera cuyo estado cambió */
    private Long parkingSpaceId;

    /** Nuevo estado de la cochera: "AVAILABLE", "RESERVED" u "OCCUPIED" */
    private String newStatus;
}
