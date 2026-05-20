package com.parkshare.repository;

import com.parkshare.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Review.
 *
 * Queries para:
 * - Reseñas de una cochera (más recientes primero)
 * - Reseñas recibidas por un usuario (reviewee)
 * - Verificación de duplicados (reservation + reviewer)
 * - Rating promedio por cochera
 * - Rating promedio por host (dashboard)
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Retorna las reseñas de una cochera específica, más recientes primero.
     */
    List<Review> findByParkingSpaceIdOrderByCreatedAtDesc(Long parkingSpaceId);

    /**
     * Retorna las reseñas recibidas por un usuario (reviewee), más recientes primero.
     */
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    /**
     * Verifica si un reviewer ya dejó una reseña para una reserva específica.
     * Usado para prevenir duplicados antes de crear.
     */
    boolean existsByReservationIdAndReviewerId(Long reservationId, Long reviewerId);

    /**
     * Calcula el promedio del rating de una cochera específica.
     * Retorna null si la cochera no tiene reseñas.
     */
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.parkingSpace.id = :parkingSpaceId
        """)
    Double averageRatingByParkingSpaceId(@Param("parkingSpaceId") Long parkingSpaceId);

    /**
     * Calcula el promedio del rating de todas las cocheras de un host.
     * Usado en el dashboard: averageRating.
     * Retorna null si el host no tiene reviews aún.
     */
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.parkingSpace.host.id = :hostId
        """)
    Double averageRatingByHostId(@Param("hostId") Long hostId);

    /**
     * Cuenta las reseñas de una cochera específica.
     */
    long countByParkingSpaceId(Long parkingSpaceId);
}
