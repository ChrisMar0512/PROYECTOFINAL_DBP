package com.parkshare.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para la creación de una nueva reserva.
 * Recibido en POST /api/reservations.
 *
 * Solo requiere el ID de la cochera — el driver se obtiene del JWT.
 */
@Data
public class CreateReservationRequest {

    /** ID de la cochera a reservar */
    @NotNull(message = "El parkingSpaceId es obligatorio")
    private Long parkingSpaceId;
}
