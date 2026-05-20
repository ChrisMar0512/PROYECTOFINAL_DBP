package com.parkshare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para la creación de una reseña.
 * Recibido en POST /api/reviews.
 */
@Data
public class CreateReviewRequest {

    /** ID de la reserva completada a reseñar */
    @NotNull(message = "El reservationId es obligatorio")
    private Long reservationId;

    /** Calificación del 1 al 5 */
    @NotNull(message = "El rating es obligatorio")
    @Min(value = 1, message = "El rating mínimo es 1")
    @Max(value = 5, message = "El rating máximo es 5")
    private Integer rating;

    /** Comentario opcional */
    private String comment;
}
