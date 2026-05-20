package com.parkshare.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una reseña individual.
 */
@Data
public class ReviewResponse {

    private Long id;

    /** Nombre del usuario que escribió la reseña */
    private String reviewerName;

    /** Nombre del usuario que recibió la reseña */
    private String revieweeName;

    /** Calificación del 1 al 5 */
    private Integer rating;

    /** Comentario opcional del reviewer */
    private String comment;

    /** Momento de creación de la reseña */
    private LocalDateTime createdAt;
}
