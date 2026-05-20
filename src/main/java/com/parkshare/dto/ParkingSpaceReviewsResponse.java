package com.parkshare.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO de respuesta con las reseñas de una cochera y su rating promedio.
 * Retornado por GET /api/reviews/parking-space/{id}.
 */
@Data
public class ParkingSpaceReviewsResponse {

    /** ID de la cochera */
    private Long parkingSpaceId;

    /** Rating promedio (1.0 - 5.0) — null si no hay reseñas */
    private Double averageRating;

    /** Cantidad total de reseñas */
    private int totalReviews;

    /** Lista de reseñas, ordenadas por más reciente primero */
    private List<ReviewResponse> reviews;
}
